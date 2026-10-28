package com.pms.module.stay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.module.folio.Folio;
import com.pms.module.folio.FolioService;
import com.pms.module.reservation.Reservation;
import com.pms.module.reservation.ReservationMapper;
import com.pms.module.reservation.ReservationRoom;
import com.pms.module.reservation.ReservationRoomMapper;
import com.pms.module.room.Room;
import com.pms.module.room.RoomService;
import com.pms.security.StaffContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 入住/退房/续住/换房。
 * 与 FolioService、RoomService 协作完成业务闭环。
 */
@Service
@RequiredArgsConstructor
public class StayService {

    private final StayMapper stayMapper;
    private final StayExtensionMapper stayExtensionMapper;
    private final RoomTransferMapper roomTransferMapper;
    private final RoomService roomService;
    private final ReservationMapper reservationMapper;
    private final ReservationRoomMapper reservationRoomMapper;
    private final FolioService folioService;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    @lombok.Data
    public static class CheckinItem {
        public Long reservationRoomId;
        public Long roomId;
    }

    @lombok.Data
    public static class CheckinReq {
        public Long reservationId;
        public List<CheckinItem> stayItems;
        public BigDecimal deposit;
        public String depositMethod;
    }

    /**
     * 办理入住：校验预订 → 占用房号 → 建 stay + folio + 押金 → 预订转 CHECKED_IN。
     */
    @Transactional
    public List<Stay> checkin(CheckinReq req) {
        if (req.getReservationId() == null || req.getStayItems() == null || req.getStayItems().isEmpty()) {
            throw new BusinessException(400, "reservationId 与 stayItems 必填");
        }
        Reservation res = reservationMapper.selectById(req.getReservationId());
        if (res == null) throw new BusinessException(404, "预订不存在");
        if ("CANCELLED".equals(res.getStatus()) || "CHECKED_OUT".equals(res.getStatus())) {
            throw BusinessException.of(409, "预订状态不可入住: " + res.getStatus());
        }

        long operator = currentOperator();
        List<Stay> created = new ArrayList<>();
        for (CheckinItem item : req.getStayItems()) {
            Room r = roomService.changeStatus(item.getRoomId(), "OCC"); // 含 VC 校验
            // 从预订房型明细取房价/房型
            BigDecimal rate = BigDecimal.ZERO;
            if (item.getReservationRoomId() != null) {
                ReservationRoom rr = reservationRoomMapper.selectById(item.getReservationRoomId());
                if (rr != null) {
                    if (rr.getRate() != null) rate = rr.getRate();
                    rr.setAssignedRoomId(r.getId()); // 回写排房
                    reservationRoomMapper.updateById(rr);
                }
            }
            Stay s = new Stay();
            s.setHotelId(hotelId);
            s.setReservationId(res.getId());
            s.setGuestId(res.getGuestId());
            s.setRoomId(r.getId());
            s.setRoomTypeId(r.getRoomTypeId());
            s.setRate(rate);
            s.setCheckInAt(LocalDateTime.now());
            s.setStatus("IN");
            s.setOperatorId(operator);
            stayMapper.insert(s);

            // 建账单并录押金
            Folio folio = folioService.ensureFolio(s);
            if (req.getDeposit() != null && req.getDeposit().compareTo(BigDecimal.ZERO) > 0) {
                folioService.addDeposit(folio.getId(), req.getDeposit(), req.getDepositMethod());
            }
            created.add(s);
        }

        res.setStatus("CHECKED_IN");
        res.setUpdatedAt(LocalDateTime.now());
        reservationMapper.updateById(res);
        return created;
    }

    public Stay get(Long id) {
        Stay s = stayMapper.selectById(id);
        if (s == null) throw new BusinessException(404, "在住记录不存在");
        return s;
    }

    public List<Stay> inHouse() {
        return stayMapper.selectList(new LambdaQueryWrapper<Stay>()
                .eq(Stay::getHotelId, hotelId)
                .eq(Stay::getStatus, "IN"));
    }

    @Transactional
    public StayExtension extend(Long stayId, LocalDate extendToDate) {
        Stay s = get(stayId);
        if (!"IN".equals(s.getStatus())) throw new BusinessException(409, "非在住不可续住");
        StayExtension e = new StayExtension();
        e.setStayId(stayId);
        e.setExtendToDate(extendToDate);
        e.setOperatorId(currentOperator());
        e.setCreatedAt(LocalDateTime.now());
        stayExtensionMapper.insert(e);
        return e;
    }

    @Transactional
    public Stay transferRoom(Long stayId, Long toRoomId, String reason) {
        Stay s = get(stayId);
        if (!"IN".equals(s.getStatus())) throw new BusinessException(409, "非在住不可换房");
        Room target = roomService.changeStatus(toRoomId, "OCC"); // 目标房必须 VC
        Long fromRoomId = s.getRoomId();

        RoomTransfer t = new RoomTransfer();
        t.setStayId(stayId);
        t.setFromRoomId(fromRoomId);
        t.setToRoomId(toRoomId);
        t.setReason(reason);
        t.setOperatorId(currentOperator());
        t.setCreatedAt(LocalDateTime.now());
        roomTransferMapper.insert(t);

        s.setRoomId(toRoomId);
        s.setRoomTypeId(target.getRoomTypeId());
        stayMapper.updateById(s);
        roomService.release(fromRoomId); // 原房 → VD
        return s;
    }

    @Transactional
    public Stay updateRate(Long stayId, BigDecimal rate) {
        Stay s = get(stayId);
        if (!"IN".equals(s.getStatus())) throw new BusinessException(409, "非在住不可调价");
        s.setRate(rate);
        stayMapper.updateById(s);
        return s;
    }

    /**
     * 退房一步结算：收款(folio) → 释放房态 → 关账 → 预订转 CHECKED_OUT。
     * @param method 支付方式 CASH/CARD/WECHAT/ALIPAY/AR
     * @param amount 实收金额；若账单余额为 0 可传 null
     */
    @Transactional
    public Stay checkout(Long stayId, String method, BigDecimal amount) {
        Stay s = get(stayId);
        if (!"IN".equals(s.getStatus())) throw new BusinessException(409, "非在住不可退房");

        // 先收款关账
        Folio folio = folioService.getByStayId(stayId);
        if (folio != null && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            folioService.pay(folio.getId(), method, amount);
        }

        s.setCheckOutAt(LocalDateTime.now());
        s.setStatus("OUT");
        stayMapper.updateById(s);
        roomService.release(s.getRoomId()); // 房态 → VD

        Reservation res = reservationMapper.selectById(s.getReservationId());
        if (res != null && "CHECKED_IN".equals(res.getStatus())) {
            res.setStatus("CHECKED_OUT");
            res.setUpdatedAt(LocalDateTime.now());
            reservationMapper.updateById(res);
        }
        return s;
    }

    /** 仅释放房态（无收款），退房请用带金额的重载。 */
    @Transactional
    public Stay checkout(Long stayId) {
        Stay s = get(stayId);
        if (!"IN".equals(s.getStatus())) throw new BusinessException(409, "非在住不可退房");
        s.setCheckOutAt(LocalDateTime.now());
        s.setStatus("OUT");
        stayMapper.updateById(s);
        roomService.release(s.getRoomId()); // 房态 → VD

        Reservation res = reservationMapper.selectById(s.getReservationId());
        if (res != null && "CHECKED_IN".equals(res.getStatus())) {
            res.setStatus("CHECKED_OUT");
            res.setUpdatedAt(LocalDateTime.now());
            reservationMapper.updateById(res);
        }
        return s;
    }

    private long currentOperator() {
        return StaffContext.get() != null ? StaffContext.get().getStaffId() : 0L;
    }
}
