package com.pms.module;

import com.pms.common.BusinessException;
import com.pms.module.folio.Folio;
import com.pms.module.folio.FolioService;
import com.pms.module.reservation.Reservation;
import com.pms.module.reservation.ReservationService;
import com.pms.module.room.Room;
import com.pms.module.room.RoomMapper;
import com.pms.module.stay.Stay;
import com.pms.module.stay.StayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 预订 → 确认 → 入住 → 账单 → 退房 全链路。
 * 钉死状态流转与跨服务协作（StayService ↔ FolioService ↔ RoomService）。
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class BusinessFlowTest {

    @Autowired ReservationService reservationService;
    @Autowired StayService stayService;
    @Autowired FolioService folioService;
    @Autowired RoomMapper roomMapper;

    private ReservationService.CreateReq newReservationReq() {
        ReservationService.CreateReq req = new ReservationService.CreateReq();
        req.setGuestName("TestGuest");
        req.setGuestPhone("13900000000");
        req.setCheckInDate(LocalDate.now());
        req.setCheckOutDate(LocalDate.now().plusDays(1));
        req.setChannelId(1L);
        ReservationService.RoomLine line = new ReservationService.RoomLine();
        line.setRoomTypeId(1L);
        line.setRoomCount(1);
        line.setRate(new BigDecimal("280"));
        req.setRooms(List.of(line));
        return req;
    }

    @Test
    void 创建预订_计算间夜数_明细落库() {
        ReservationService.Detail d = reservationService.create(newReservationReq());

        assertThat(d.reservation.getResNo()).startsWith("R");
        assertThat(d.reservation.getStatus()).isEqualTo("PENDING");
        assertThat(d.reservation.getNights()).isEqualTo(1);
        assertThat(d.rooms).hasSize(1);
        assertThat(d.rooms.get(0).getRate()).isEqualByComparingTo("280");
    }

    @Test
    void 日期非法应拒绝() {
        ReservationService.CreateReq req = newReservationReq();
        req.setCheckOutDate(req.getCheckInDate()); // 离店==入住
        assertThatThrownBy(() -> reservationService.create(req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 确认预订_状态流转_重复确认拒绝() {
        ReservationService.Detail d = reservationService.create(newReservationReq());

        Reservation confirmed = reservationService.confirm(d.reservation.getId());
        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");

        assertThatThrownBy(() -> reservationService.confirm(d.reservation.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 入住退房全链路() {
        // 预订 + 确认
        ReservationService.Detail d = reservationService.create(newReservationReq());
        Long resRoomId = d.rooms.get(0).getId();
        reservationService.confirm(d.reservation.getId());

        // 找一间 打开+净房
        Room vacant = roomMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Room>()
                        .eq(Room::getHotelId, 1L)
                        .eq(Room::getRentStatus, "OPEN")
                        .eq(Room::getCleanStatus, "CLEAN")).get(0);

        // 入住：押金 500
        StayService.CheckinReq ci = new StayService.CheckinReq();
        ci.setReservationId(d.reservation.getId());
        StayService.CheckinItem item = new StayService.CheckinItem();
        item.setReservationRoomId(resRoomId);
        item.setRoomId(vacant.getId());
        ci.setStayItems(List.of(item));
        ci.setDeposit(new BigDecimal("500"));
        ci.setDepositMethod("CASH");

        List<Stay> stays = stayService.checkin(ci);
        assertThat(stays).hasSize(1);
        Stay stay = stays.get(0);
        assertThat(stay.getStatus()).isEqualTo("IN");

        // 房态 → 已排房 ASSIGNED
        Room occupied = roomMapper.selectById(vacant.getId());
        assertThat(occupied.getRentStatus()).isEqualTo("ASSIGNED");

        // 预订 → CHECKED_IN
        Reservation afterCi = reservationService.get(d.reservation.getId());
        assertThat(afterCi.getStatus()).isEqualTo("CHECKED_IN");

        // 账单存在且押金已入账（-500），仍 OPEN
        Folio folio = folioService.getByStayId(stay.getId());
        assertThat(folio).isNotNull();
        assertThat(folio.getBalance()).isEqualByComparingTo(new BigDecimal("-500"));
        assertThat(folio.getStatus()).isEqualTo("OPEN");

        // 录房费 800 → 余额 +300（消费超过押金，产生应收）
        folioService.addCharge(folio.getId(), "room-charge", new BigDecimal("800"), "房费");
        Folio charged = folioService.getByStayId(stay.getId());
        assertThat(charged.getBalance()).isEqualByComparingTo(new BigDecimal("300"));

        // 退房：收 300，余额清零关账
        Stay checkedOut = stayService.checkout(stay.getId(), "WECHAT", new BigDecimal("300"));
        assertThat(checkedOut.getStatus()).isEqualTo("OUT");
        assertThat(checkedOut.getCheckOutAt()).isNotNull();

        // 房态 → 释放：可售打开，卫生脏房
        Room released = roomMapper.selectById(vacant.getId());
        assertThat(released.getRentStatus()).isEqualTo("OPEN");
        assertThat(released.getCleanStatus()).isEqualTo("DIRTY");

        // 预订 → CHECKED_OUT
        Reservation afterCo = reservationService.get(d.reservation.getId());
        assertThat(afterCo.getStatus()).isEqualTo("CHECKED_OUT");

        // 账单 → CLOSED
        Folio closed = folioService.detail(folio.getId()).folio;
        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(closed.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 非在住不可退房() {
        ReservationService.Detail d = reservationService.create(newReservationReq());
        reservationService.confirm(d.reservation.getId());

        // 未入住直接退房：stay 不存在 → 404
        assertThatThrownBy(() -> stayService.checkout(999999L, "CASH", BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 取消已入住预订应拒绝() {
        ReservationService.Detail d = reservationService.create(newReservationReq());
        reservationService.confirm(d.reservation.getId());
        Room vacant = roomMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Room>()
                        .eq(Room::getHotelId, 1L)
                        .eq(Room::getRentStatus, "OPEN")
                        .eq(Room::getCleanStatus, "CLEAN")).get(0);
        StayService.CheckinReq ci = new StayService.CheckinReq();
        ci.setReservationId(d.reservation.getId());
        StayService.CheckinItem item = new StayService.CheckinItem();
        item.setReservationRoomId(d.rooms.get(0).getId());
        item.setRoomId(vacant.getId());
        ci.setStayItems(List.of(item));
        ci.setDeposit(new BigDecimal("500"));
        ci.setDepositMethod("CASH");
        stayService.checkin(ci);

        assertThatThrownBy(() -> reservationService.cancel(d.reservation.getId(), "误订"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 撤销入住_回滚在住与账务_房态恢复净房() {
        // 预订 + 确认 + 入住（押金 500）
        ReservationService.Detail d = reservationService.create(newReservationReq());
        reservationService.confirm(d.reservation.getId());
        Room vacant = roomMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Room>()
                        .eq(Room::getHotelId, 1L)
                        .eq(Room::getRentStatus, "OPEN")
                        .eq(Room::getCleanStatus, "CLEAN")).get(0);
        StayService.CheckinReq ci = new StayService.CheckinReq();
        ci.setReservationId(d.reservation.getId());
        StayService.CheckinItem item = new StayService.CheckinItem();
        item.setReservationRoomId(d.rooms.get(0).getId());
        item.setRoomId(vacant.getId());
        ci.setStayItems(List.of(item));
        ci.setDeposit(new BigDecimal("500"));
        ci.setDepositMethod("CASH");
        Stay stay = stayService.checkin(ci).get(0);

        // 撤销入住
        stayService.undoCheckin(stay.getId());

        // 在住已删 → get 抛 404
        assertThatThrownBy(() -> stayService.get(stay.getId()))
                .isInstanceOf(BusinessException.class);
        // 账单已删 → getByStayId 返回 null
        assertThat(folioService.getByStayId(stay.getId())).isNull();
        // 房态恢复 打开+净房（注意：非退房的脏房）
        Room reverted = roomMapper.selectById(vacant.getId());
        assertThat(reverted.getRentStatus()).isEqualTo("OPEN");
        assertThat(reverted.getCleanStatus()).isEqualTo("CLEAN");
        // 预订回退 → CONFIRMED（待入住）
        Reservation after = reservationService.get(d.reservation.getId());
        assertThat(after.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void 有续住记录不可撤销入住() {
        ReservationService.Detail d = reservationService.create(newReservationReq());
        reservationService.confirm(d.reservation.getId());
        Room vacant = roomMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Room>()
                        .eq(Room::getHotelId, 1L)
                        .eq(Room::getRentStatus, "OPEN")
                        .eq(Room::getCleanStatus, "CLEAN")).get(0);
        StayService.CheckinReq ci = new StayService.CheckinReq();
        ci.setReservationId(d.reservation.getId());
        StayService.CheckinItem item = new StayService.CheckinItem();
        item.setReservationRoomId(d.rooms.get(0).getId());
        item.setRoomId(vacant.getId());
        ci.setStayItems(List.of(item));
        ci.setDeposit(new BigDecimal("500"));
        ci.setDepositMethod("CASH");
        Stay stay = stayService.checkin(ci).get(0);

        // 续住 1 晚（无收款）
        stayService.extend(stay.getId(), 1, null, null, null);

        // 撤销入住应被拒绝
        assertThatThrownBy(() -> stayService.undoCheckin(stay.getId()))
                .isInstanceOf(BusinessException.class);
        // 在住仍在
        assertThat(stayService.get(stay.getId()).getStatus()).isEqualTo("IN");
    }
}
