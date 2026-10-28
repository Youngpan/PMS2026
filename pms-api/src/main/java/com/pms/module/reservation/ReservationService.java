package com.pms.module.reservation;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.BusinessException;
import com.pms.common.PageResult;
import com.pms.security.StaffContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final ReservationRoomMapper reservationRoomMapper;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    // ---------- DTO ----------

    @Data
    public static class CreateReq {
        public String guestName;
        public String guestPhone;
        public LocalDate checkInDate;
        public LocalDate checkOutDate;
        public Long channelId;
        public String remark;
        public List<RoomLine> rooms;
    }

    @Data
    public static class RoomLine {
        public Long roomTypeId;
        public Integer roomCount;
        public java.math.BigDecimal rate;
        public Long assignedRoomId;
    }

    @Data
    public static class Detail {
        public Reservation reservation;
        public List<ReservationRoom> rooms;
    }

    // ---------- 创建 ----------

    @Transactional
    public Detail create(CreateReq req) {
        if (req.getCheckInDate() == null || req.getCheckOutDate() == null
                || !req.getCheckOutDate().isAfter(req.getCheckInDate())) {
            throw new BusinessException("入住/离店日期不合法");
        }
        if (req.getRooms() == null || req.getRooms().isEmpty()) {
            throw new BusinessException("至少需要一条房型明细");
        }

        Reservation r = new Reservation();
        r.setHotelId(hotelId);
        r.setResNo("R" + System.currentTimeMillis() + RandomUtil.randomNumbers(2));
        r.setGuestName(req.getGuestName());
        r.setGuestPhone(req.getGuestPhone());
        r.setCheckInDate(req.getCheckInDate());
        r.setCheckOutDate(req.getCheckOutDate());
        r.setNights((int) (req.getCheckOutDate().toEpochDay() - req.getCheckInDate().toEpochDay()));
        r.setChannelId(req.getChannelId());
        r.setStatus("PENDING");
        r.setBookerId(StaffContext.get() != null ? StaffContext.get().getStaffId() : 0L);
        r.setRemark(req.getRemark());
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        reservationMapper.insert(r);

        for (RoomLine line : req.getRooms()) {
            ReservationRoom rr = new ReservationRoom();
            rr.setReservationId(r.getId());
            rr.setRoomTypeId(line.getRoomTypeId());
            rr.setRoomCount(line.getRoomCount() == null ? 1 : line.getRoomCount());
            rr.setRate(line.getRate());
            rr.setAssignedRoomId(line.getAssignedRoomId());
            reservationRoomMapper.insert(rr);
        }
        return detail(r.getId());
    }

    // ---------- 查询 ----------

    public PageResult<Reservation> page(long pageNo, long pageSize, String status, String phone, LocalDate from, LocalDate to) {
        Page<Reservation> p = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Reservation> w = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getHotelId, hotelId)
                .eq(status != null && !status.isEmpty(), Reservation::getStatus, status)
                .eq(phone != null && !phone.isEmpty(), Reservation::getGuestPhone, phone)
                .ge(from != null, Reservation::getCheckInDate, from)
                .le(to != null, Reservation::getCheckInDate, to)
                .orderByDesc(Reservation::getCreatedAt);
        reservationMapper.selectPage(p, w);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    public Reservation get(Long id) {
        Reservation r = reservationMapper.selectById(id);
        if (r == null) throw new BusinessException(404, "预订不存在");
        return r;
    }

    public Detail detail(Long id) {
        Reservation r = get(id);
        List<ReservationRoom> rooms = reservationRoomMapper.selectList(new LambdaQueryWrapper<ReservationRoom>()
                .eq(ReservationRoom::getReservationId, id));
        Detail d = new Detail();
        d.setReservation(r);
        d.setRooms(rooms);
        return d;
    }

    // ---------- 状态流转 ----------

    @Transactional
    public Reservation confirm(Long id) {
        Reservation r = get(id);
        if (!"PENDING".equals(r.getStatus())) {
            throw BusinessException.of(409, "当前状态不可确认: " + r.getStatus());
        }
        r.setStatus("CONFIRMED");
        r.setUpdatedAt(LocalDateTime.now());
        reservationMapper.updateById(r);
        return r;
    }

    @Transactional
    public Reservation cancel(Long id, String reason) {
        Reservation r = get(id);
        if ("CHECKED_IN".equals(r.getStatus()) || "CHECKED_OUT".equals(r.getStatus())) {
            throw BusinessException.of(409, "已入住/退房不可取消");
        }
        r.setStatus("CANCELLED");
        r.setRemark(reason);
        r.setUpdatedAt(LocalDateTime.now());
        reservationMapper.updateById(r);
        return r;
    }

    /** 排房：为某条房型明细绑定具体房号。 */
    @Transactional
    public ReservationRoom assignRoom(Long reservationRoomId, Long roomId) {
        ReservationRoom rr = reservationRoomMapper.selectById(reservationRoomId);
        if (rr == null) throw new BusinessException(404, "预订房型明细不存在");
        rr.setAssignedRoomId(roomId);
        reservationRoomMapper.updateById(rr);
        return rr;
    }
}
