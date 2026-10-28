package com.pms.module.housekeeping;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.module.housekeeping.HousekeepingService.CalendarBoard;
import com.pms.module.housekeeping.HousekeepingService.Cell;
import com.pms.module.reservation.ReservationService;
import com.pms.module.room.Room;
import com.pms.module.room.RoomMapper;
import com.pms.module.stay.Stay;
import com.pms.module.stay.StayExtension;
import com.pms.module.stay.StayExtensionMapper;
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

/**
 * 房态日历 + 钟点房 + 续住(带收款) 新功能回归。
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class HousekeepingCalendarTest {

    @Autowired HousekeepingService housekeepingService;
    @Autowired ReservationService reservationService;
    @Autowired StayService stayService;
    @Autowired RoomMapper roomMapper;
    @Autowired StayExtensionMapper stayExtensionMapper;

    private Room vacantRoom() {
        return roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getHotelId, 1L)
                .eq(Room::getRentStatus, "OPEN")
                .eq(Room::getCleanStatus, "CLEAN")
                .orderByAsc(Room::getNumber)).get(0);
    }

    @Test
    void 日历空库返回房间和日期无格子() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(6);
        CalendarBoard board = housekeepingService.calendar(from, to);

        assertThat(board.getRooms()).isNotEmpty();
        assertThat(board.getDates()).hasSize(7);
        assertThat(board.getDates().get(0)).isEqualTo(from.toString());
        assertThat(board.getCells()).isEmpty();
    }

    @Test
    void 确认预订排房后日历格显示reservation() {
        // 建单 + 确认 + 排房到一间净房
        ReservationService.CreateReq req = new ReservationService.CreateReq();
        req.setGuestName("CalGuest");
        req.setGuestPhone("13900001111");
        req.setCheckInDate(LocalDate.now());
        req.setCheckOutDate(LocalDate.now().plusDays(2));
        req.setChannelId(1L);
        ReservationService.RoomLine line = new ReservationService.RoomLine();
        line.setRoomTypeId(1L);
        line.setRoomCount(1);
        line.setRate(new BigDecimal("280"));
        req.setRooms(List.of(line));
        ReservationService.Detail d = reservationService.create(req);
        reservationService.confirm(d.reservation.getId());

        Room r = vacantRoom();
        reservationService.assignRoom(d.rooms.get(0).getId(), r.getId());

        CalendarBoard board = housekeepingService.calendar(LocalDate.now(), LocalDate.now().plusDays(2));
        Cell cell = board.getCells().get(r.getId() + ":" + LocalDate.now());
        assertThat(cell).as("今日格应有预订占用").isNotNull();
        assertThat(cell.getType()).isEqualTo("reservation");
        assertThat(cell.getGuestName()).isEqualTo("CalGuest");
        // checkOut 当日不覆盖（exclusive）
        assertThat(board.getCells()).doesNotContainKey(r.getId() + ":" + LocalDate.now().plusDays(2));
    }

    @Test
    void 钟点房创建后日历格显示stay且续住可加天数() {
        Room r = vacantRoom();
        Stay s = stayService.createHourlyStay(r.getId(), "HourlyGuest", "13900002222",
                3, new BigDecimal("50"), "CASH", new BigDecimal("150"));

        assertThat(s.getType()).isEqualTo("HOURLY");
        assertThat(s.getHours()).isEqualTo(3);
        Room after = roomMapper.selectById(r.getId());
        assertThat(after.getRentStatus()).isEqualTo("ASSIGNED");

        CalendarBoard board = housekeepingService.calendar(LocalDate.now(), LocalDate.now());
        Cell cell = board.getCells().get(r.getId() + ":" + LocalDate.now());
        assertThat(cell).isNotNull();
        assertThat(cell.getType()).isEqualTo("stay");
        assertThat(cell.isHourly()).isTrue();

        // 续住 2 天，收款 100
        StayExtension ext = stayService.extend(s.getId(), 2, new BigDecimal("100"), "WECHAT", "续住备注");
        assertThat(ext.getExtendToDate()).isNotNull();
        assertThat(ext.getRemark()).isEqualTo("续住备注");
        List<StayExtension> exts = stayExtensionMapper.selectList(
                new LambdaQueryWrapper<StayExtension>().eq(StayExtension::getStayId, s.getId()));
        assertThat(exts).hasSize(1);
    }
}
