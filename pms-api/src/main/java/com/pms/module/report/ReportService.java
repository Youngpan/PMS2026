package com.pms.module.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.module.folio.Folio;
import com.pms.module.folio.FolioMapper;
import com.pms.module.folio.Payment;
import com.pms.module.folio.PaymentMapper;
import com.pms.module.room.Room;
import com.pms.module.room.RoomMapper;
import com.pms.module.stay.Stay;
import com.pms.module.stay.StayMapper;
import com.pms.security.StaffContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 夜审与报表：夜审草稿/确认、营收日报、客账余额、出租率/ADR/RevPAR。
 * 说明：房费由前台/财务在账务模块手工录入，本系统不自动过账。
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final NightAuditMapper nightAuditMapper;
    private final StayMapper stayMapper;
    private final PaymentMapper paymentMapper;
    private final FolioMapper folioMapper;
    private final RoomMapper roomMapper;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    // ---------- 夜审 ----------

    @Data
    public static class NightAuditDraft {
        public LocalDate date;
        public int checkins;
        public int checkouts;
        public BigDecimal revenue;
        public int inHouse;
    }

    public NightAuditDraft nightAuditDraft(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        long checkins = stayMapper.selectCount(new LambdaQueryWrapper<Stay>()
                .eq(Stay::getHotelId, hotelId)
                .ge(Stay::getCheckInAt, dayStart)
                .lt(Stay::getCheckInAt, dayEnd));
        long checkouts = stayMapper.selectCount(new LambdaQueryWrapper<Stay>()
                .eq(Stay::getHotelId, hotelId)
                .ge(Stay::getCheckOutAt, dayStart)
                .lt(Stay::getCheckOutAt, dayEnd));
        long inHouse = stayMapper.selectCount(new LambdaQueryWrapper<Stay>()
                .eq(Stay::getHotelId, hotelId)
                .eq(Stay::getStatus, "IN"));

        BigDecimal revenue = sumPayments(dayStart, dayEnd);

        NightAuditDraft d = new NightAuditDraft();
        d.date = date;
        d.checkins = (int) checkins;
        d.checkouts = (int) checkouts;
        d.inHouse = (int) inHouse;
        d.revenue = revenue;
        return d;
    }

    @Transactional
    public NightAudit confirmNightAudit(LocalDate date) {
        NightAudit exist = nightAuditMapper.selectOne(new LambdaQueryWrapper<NightAudit>()
                .eq(NightAudit::getHotelId, hotelId)
                .eq(NightAudit::getAuditDate, date)
                .last("LIMIT 1"));
        if (exist != null && "CONFIRMED".equals(exist.getStatus())) {
            throw new BusinessException(409, "该日已确认夜审");
        }
        NightAuditDraft draft = nightAuditDraft(date);

        NightAudit na = exist != null ? exist : new NightAudit();
        na.setHotelId(hotelId);
        na.setAuditDate(date);
        na.setCheckinsCount(draft.checkins);
        na.setCheckoutsCount(draft.checkouts);
        na.setRevenue(draft.revenue);
        na.setAuditedBy(StaffContext.get() != null ? StaffContext.get().getStaffId() : 0L);
        na.setStatus("CONFIRMED");
        na.setCreatedAt(exist != null ? exist.getCreatedAt() : LocalDateTime.now());
        if (exist != null) nightAuditMapper.updateById(na);
        else nightAuditMapper.insert(na);
        return na;
    }

    // ---------- 营收日报 ----------

    @Data
    public static class RevenueReport {
        public BigDecimal total;
        public List<MethodBreakdown> byMethod;
    }

    @Data
    public static class MethodBreakdown {
        public String method;
        public BigDecimal amount;
    }

    public RevenueReport revenueReport(LocalDate from, LocalDate to) {
        LocalDateTime tFrom = from.atStartOfDay();
        LocalDateTime tTo = to.plusDays(1).atStartOfDay();
        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .ge(Payment::getPaidAt, tFrom)
                .lt(Payment::getPaidAt, tTo));

        BigDecimal total = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<MethodBreakdown> byMethod = payments.stream()
                .collect(Collectors.groupingBy(Payment::getMethod,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)))
                .entrySet().stream().map(e -> {
                    MethodBreakdown b = new MethodBreakdown();
                    b.method = e.getKey();
                    b.amount = e.getValue();
                    return b;
                }).collect(Collectors.toList());

        RevenueReport r = new RevenueReport();
        r.total = total;
        r.byMethod = byMethod;
        return r;
    }

    // ---------- 客账余额表 ----------

    public List<Folio> folioBalance() {
        return folioMapper.selectList(new LambdaQueryWrapper<Folio>()
                .eq(Folio::getHotelId, hotelId)
                .eq(Folio::getStatus, "OPEN")
                .gt(Folio::getBalance, BigDecimal.ZERO)
                .orderByDesc(Folio::getBalance));
    }

    // ---------- 每日营收趋势 ----------

    @Data
    public static class DailyRevenue {
        public String date;
        public BigDecimal amount;
    }

    public List<DailyRevenue> revenueDaily(LocalDate from, LocalDate to) {
        LocalDateTime tFrom = from.atStartOfDay();
        LocalDateTime tTo = to.plusDays(1).atStartOfDay();
        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .ge(Payment::getPaidAt, tFrom)
                .lt(Payment::getPaidAt, tTo));

        java.util.Map<LocalDate, BigDecimal> byDate = new java.util.TreeMap<>();
        for (long d = 0; d <= to.toEpochDay() - from.toEpochDay(); d++) {
            byDate.put(from.plusDays(d), BigDecimal.ZERO);
        }
        for (Payment p : payments) {
            LocalDate day = p.getPaidAt().toLocalDate();
            byDate.merge(day, p.getAmount(), BigDecimal::add);
        }
        return byDate.entrySet().stream().map(e -> {
            DailyRevenue r = new DailyRevenue();
            r.date = e.getKey().toString();
            r.amount = e.getValue();
            return r;
        }).collect(Collectors.toList());
    }

    // ---------- 出租率 / ADR / RevPAR ----------

    @Data
    public static class OccupancyReport {
        public LocalDate from;
        public LocalDate to;
        public int days;
        public int roomCount;
        public double soldRoomNights;
        public double occupancy; // soldRoomNights / availableRoomNights
        public BigDecimal adr;   // 客房收入 / soldRoomNights
        public BigDecimal revpar; // 客房收入 / availableRoomNights
    }

    public OccupancyReport occupancy(LocalDate from, LocalDate to) {
        long roomCount = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getHotelId, hotelId)
                .isNull(Room::getDeletedAt));
        int days = (int) (to.toEpochDay() - from.toEpochDay() + 1);
        double availableRoomNights = Math.max(roomCount, 0) * days;

        // 取与区间有交集的入住记录
        LocalDateTime tFrom = from.atStartOfDay();
        LocalDateTime tTo = to.plusDays(1).atStartOfDay();
        List<Stay> stays = stayMapper.selectList(new LambdaQueryWrapper<Stay>()
                .eq(Stay::getHotelId, hotelId)
                .lt(Stay::getCheckInAt, tTo)
                .and(w -> w.isNull(Stay::getCheckOutAt).or().ge(Stay::getCheckOutAt, tFrom)));

        double soldRoomNights = 0;
        BigDecimal roomRevenue = BigDecimal.ZERO;
        for (Stay s : stays) {
            LocalDate ci = s.getCheckInAt().toLocalDate();
            LocalDate co = s.getCheckOutAt() == null ? to : s.getCheckOutAt().toLocalDate();
            LocalDate effStart = ci.isBefore(from) ? from : ci;
            LocalDate effEnd = co.isAfter(to) ? to : co;
            long nights = effEnd.toEpochDay() - effStart.toEpochDay() + 1;
            if (nights <= 0) continue;
            soldRoomNights += nights;
            if (s.getRate() != null) {
                roomRevenue = roomRevenue.add(s.getRate().multiply(BigDecimal.valueOf(nights)));
            }
        }

        OccupancyReport r = new OccupancyReport();
        r.from = from;
        r.to = to;
        r.days = days;
        r.roomCount = (int) roomCount;
        r.soldRoomNights = soldRoomNights;
        r.occupancy = availableRoomNights == 0 ? 0 : soldRoomNights / availableRoomNights;
        r.adr = soldRoomNights == 0 ? BigDecimal.ZERO
                : roomRevenue.divide(BigDecimal.valueOf(soldRoomNights), 2, RoundingMode.HALF_UP);
        r.revpar = availableRoomNights == 0 ? BigDecimal.ZERO
                : roomRevenue.divide(BigDecimal.valueOf(availableRoomNights), 2, RoundingMode.HALF_UP);
        return r;
    }

    private BigDecimal sumPayments(LocalDateTime from, LocalDateTime to) {
        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .ge(Payment::getPaidAt, from)
                .lt(Payment::getPaidAt, to));
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
