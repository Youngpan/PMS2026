package com.pms.module.housekeeping;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.module.reservation.Reservation;
import com.pms.module.reservation.ReservationMapper;
import com.pms.module.reservation.ReservationRoom;
import com.pms.module.reservation.ReservationRoomMapper;
import com.pms.module.room.Room;
import com.pms.module.room.RoomMapper;
import com.pms.module.stay.Stay;
import com.pms.module.stay.StayMapper;
import com.pms.security.StaffContext;
import com.pms.ws.RoomStatusPublisher;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 房务：房态看板、清扫派单、维修报事。
 */
@Service
@RequiredArgsConstructor
public class HousekeepingService {

    private final HousekeepingTaskMapper taskMapper;
    private final MaintenanceMapper maintenanceMapper;
    private final RoomMapper roomMapper;
    private final StayMapper stayMapper;
    private final ReservationMapper reservationMapper;
    private final ReservationRoomMapper reservationRoomMapper;
    private final RoomStatusPublisher publisher;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    /** 房态看板：按楼层聚合。 */
    public List<FloorBoard> dashboard() {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getHotelId, hotelId)
                .orderByAsc(Room::getFloor, Room::getNumber));
        Map<Integer, FloorBoard> byFloor = new LinkedHashMap<>();
        for (Room r : rooms) {
            byFloor.computeIfAbsent(r.getFloor(), f -> {
                FloorBoard b = new FloorBoard();
                b.setFloor(f);
                return b;
            }).getRooms().add(r);
        }
        return List.copyOf(byFloor.values());
    }

    /**
     * 房态日历：返回 rooms（含 rent/clean/remark）、dates、cells。
     * cells 仅包含被在住 stay 或已排房 reservation 覆盖的 (roomId,date) 格子。
     * 日期范围由调用方指定（前端默认昨天起 20 天）。
     */
    public CalendarBoard calendar(LocalDate from, LocalDate to) {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getHotelId, hotelId)
                .orderByAsc(Room::getFloor, Room::getNumber));
        List<String> dates = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) dates.add(d.toString());

        // 在住 stay（status=IN，且时间窗与 [from,to] 重叠）
        List<Stay> stays = stayMapper.selectList(new LambdaQueryWrapper<Stay>()
                .eq(Stay::getHotelId, hotelId)
                .eq(Stay::getStatus, "IN")
                .le(Stay::getCheckInAt, to.atTime(23, 59, 59))
                .and(w -> w.isNull(Stay::getCheckOutAt).or().ge(Stay::getCheckOutAt, from.atStartOfDay())));
        Map<Long, Stay> stayByRoom = stays.stream()
                .collect(Collectors.toMap(Stay::getRoomId, s -> s, (a, b) -> a));

        // 已排房 reservation_room（assigned_room_id 非空）
        List<ReservationRoom> rrs = reservationRoomMapper.selectList(new LambdaQueryWrapper<ReservationRoom>()
                .isNotNull(ReservationRoom::getAssignedRoomId));
        Map<Long, ReservationRoom> rrByRoom = rrs.stream()
                .collect(Collectors.toMap(ReservationRoom::getAssignedRoomId, r -> r, (a, b) -> a));
        Map<Long, Reservation> resById = Map.of();
        if (!rrs.isEmpty()) {
            List<Long> resIds = rrs.stream().map(ReservationRoom::getReservationId).distinct().collect(Collectors.toList());
            List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                    .in(Reservation::getId, resIds)
                    .in(Reservation::getStatus, List.of("CONFIRMED", "CHECKED_IN"))
                    .le(Reservation::getCheckInDate, to)
                    .gt(Reservation::getCheckOutDate, from));
            resById = reservations.stream().collect(Collectors.toMap(Reservation::getId, r -> r, (a, b) -> a));
        }

        Map<String, Cell> cells = new LinkedHashMap<>();
        for (Room r : rooms) {
            Stay st = stayByRoom.get(r.getId());
            ReservationRoom rr = rrByRoom.get(r.getId());
            Reservation res = rr == null ? null : resById.get(rr.getReservationId());
            for (String d : dates) {
                LocalDate date = LocalDate.parse(d);
                if (st != null && coversStay(st, date)) {
                    cells.put(r.getId() + ":" + d, cellOfStay(st, r.getId(), d));
                } else if (res != null && coversReservation(res, date)) {
                    cells.put(r.getId() + ":" + d, cellOfReservation(res, r.getId(), d));
                }
            }
        }

        CalendarBoard board = new CalendarBoard();
        board.setRooms(rooms);
        board.setDates(dates);
        board.setCells(cells);
        return board;
    }

    /**
     * stay 覆盖某日：checkInDate ≤ date ≤ checkOutDate（含退房当日，因退房当天客人仍在至退房时刻；
     * 钟点房 checkOut 同日也覆盖）。未退房则 checkOutDate 视为 +∞。
     */
    private boolean coversStay(Stay s, LocalDate date) {
        LocalDate ci = s.getCheckInAt() == null ? null : s.getCheckInAt().toLocalDate();
        if (ci == null || date.isBefore(ci)) return false;
        if (s.getCheckOutAt() != null) {
            return !date.isAfter(s.getCheckOutAt().toLocalDate());
        }
        return true;
    }

    /** reservation 覆盖某日：checkInDate ≤ date ＜ checkOutDate。 */
    private boolean coversReservation(Reservation res, LocalDate date) {
        if (date.isBefore(res.getCheckInDate())) return false;
        return date.isBefore(res.getCheckOutDate());
    }

    private Cell cellOfStay(Stay s, Long roomId, String date) {
        Cell c = new Cell();
        c.setType("stay");
        c.setId(s.getId());
        c.setRoomId(roomId);
        c.setDate(date);
        c.setStatus(s.getStatus());
        c.setGuestName(s.getGuestName());
        c.setCheckIn(s.getCheckInAt() == null ? null : s.getCheckInAt().toLocalDate().toString());
        c.setCheckOut(s.getCheckOutAt() == null ? null : s.getCheckOutAt().toLocalDate().toString());
        c.setHourly("HOURLY".equals(s.getType()));
        return c;
    }

    private Cell cellOfReservation(Reservation res, Long roomId, String date) {
        Cell c = new Cell();
        c.setType("reservation");
        c.setId(res.getId());
        c.setRoomId(roomId);
        c.setDate(date);
        c.setStatus(res.getStatus());
        c.setGuestName(res.getGuestName());
        c.setCheckIn(res.getCheckInDate().toString());
        c.setCheckOut(res.getCheckOutDate().toString());
        c.setHourly(false);
        return c;
    }

    public List<HousekeepingTask> tasks(String status, Long assigneeId) {
        return taskMapper.selectList(new LambdaQueryWrapper<HousekeepingTask>()
                .eq(HousekeepingTask::getHotelId, hotelId)
                .eq(status != null, HousekeepingTask::getStatus, status)
                .eq(assigneeId != null, HousekeepingTask::getAssigneeId, assigneeId)
                .orderByAsc(HousekeepingTask::getCreatedAt));
    }

    @Transactional
    public HousekeepingTask createTask(Long roomId, String type, Long assigneeId) {
        HousekeepingTask t = new HousekeepingTask();
        t.setHotelId(hotelId);
        t.setRoomId(roomId);
        t.setType(type == null ? "DAILY" : type);
        t.setStatus("PENDING");
        t.setAssigneeId(assigneeId);
        t.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(t);
        return t;
    }

    /** 完成清扫：任务置 DONE，房态 VD→VC。 */
    @Transactional
    public HousekeepingTask finishTask(Long taskId) {
        HousekeepingTask t = taskMapper.selectById(taskId);
        if (t == null) throw new BusinessException(404, "清扫任务不存在");
        if (!"PENDING".equals(t.getStatus())) throw new BusinessException(409, "任务已完结");
        t.setStatus("DONE");
        t.setFinishedAt(LocalDateTime.now());
        taskMapper.updateById(t);

        Room r = roomMapper.selectById(t.getRoomId());
        if (r != null && "DIRTY".equals(r.getCleanStatus())) {
            r.setCleanStatus("CLEAN");
            roomMapper.updateById(r);
            publisher.publish(r);
        }
        return t;
    }

    /** 报修：建维修单，房可售置 CLOSED（关闭）。 */
    @Transactional
    public Maintenance reportMaintenance(Long roomId, String issue) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        r.setRentStatus("CLOSED");
        roomMapper.updateById(r);
        publisher.publish(r);

        Maintenance m = new Maintenance();
        m.setHotelId(hotelId);
        m.setRoomId(roomId);
        m.setIssue(issue);
        m.setStatus("OPEN");
        m.setReporterId(currentOperator());
        m.setCreatedAt(LocalDateTime.now());
        maintenanceMapper.insert(m);
        return m;
    }

    /** 修复：维修单置 FIXED，房态恢复 VC。 */
    @Transactional
    public Maintenance fixMaintenance(Long maintenanceId) {
        Maintenance m = maintenanceMapper.selectById(maintenanceId);
        if (m == null) throw new BusinessException(404, "维修单不存在");
        if (!"OPEN".equals(m.getStatus())) throw new BusinessException(409, "维修单已完结");
        m.setStatus("FIXED");
        m.setFixedAt(LocalDateTime.now());
        maintenanceMapper.updateById(m);

        Room r = roomMapper.selectById(m.getRoomId());
        if (r != null && "CLOSED".equals(r.getRentStatus())) {
            r.setRentStatus("OPEN");
            roomMapper.updateById(r);
            publisher.publish(r);
        }
        return m;
    }

    public List<Maintenance> maintenances(String status) {
        return maintenanceMapper.selectList(new LambdaQueryWrapper<Maintenance>()
                .eq(Maintenance::getHotelId, hotelId)
                .eq(status != null, Maintenance::getStatus, status)
                .orderByDesc(Maintenance::getCreatedAt));
    }

    private long currentOperator() {
        return StaffContext.get() != null ? StaffContext.get().getStaffId() : 0L;
    }

    @Data
    public static class FloorBoard {
        private Integer floor;
        private final List<Room> rooms = new java.util.ArrayList<>();
    }

    @Data
    public static class CalendarBoard {
        private List<Room> rooms;
        private List<String> dates;
        /** key = "<roomId>:<date>"，仅含被订单/在住覆盖的格子 */
        private Map<String, Cell> cells;
    }

    @Data
    public static class Cell {
        /** stay | reservation */
        private String type;
        private Long id;
        private Long roomId;
        private String date;
        private String status;
        private String guestName;
        private String checkIn;
        private String checkOut;
        private boolean hourly;
    }
}
