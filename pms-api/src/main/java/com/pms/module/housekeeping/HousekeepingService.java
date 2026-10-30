package com.pms.module.housekeeping;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.module.channel.Channel;
import com.pms.module.channel.ChannelMapper;
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
    private final RoomBlockMapper roomBlockMapper;
    private final RoomMapper roomMapper;
    private final StayMapper stayMapper;
    private final ReservationMapper reservationMapper;
    private final ReservationRoomMapper reservationRoomMapper;
    private final ChannelMapper channelMapper;
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

        // 渠道映射：id → Channel（cell 渠道展示用）
        Map<Long, Channel> channelById = channelMapper.selectList(new LambdaQueryWrapper<Channel>()
                .orderByAsc(Channel::getId)).stream()
                .collect(Collectors.toMap(Channel::getId, c -> c, (a, b) -> a));

        // 在住/已退房 stay（status=IN/OUT，且时间窗与 [from,to] 重叠）
        List<Stay> stays = stayMapper.selectList(new LambdaQueryWrapper<Stay>()
                .eq(Stay::getHotelId, hotelId)
                .in(Stay::getStatus, List.of("IN", "OUT"))
                .le(Stay::getCheckInAt, to.atTime(23, 59, 59))
                .and(w -> w.isNull(Stay::getCheckOutAt).or().ge(Stay::getCheckOutAt, from.atStartOfDay())));
        Map<Long, Stay> stayByRoom = stays.stream()
                .collect(Collectors.toMap(Stay::getRoomId, s -> s, (a, b) -> a));

        // 已排房 reservation_room（assigned_room_id 非空）
        List<ReservationRoom> rrs = reservationRoomMapper.selectList(new LambdaQueryWrapper<ReservationRoom>()
                .isNotNull(ReservationRoom::getAssignedRoomId));
        Map<Long, ReservationRoom> rrByRoom = rrs.stream()
                .collect(Collectors.toMap(ReservationRoom::getAssignedRoomId, r -> r, (a, b) -> a));

        // 合并加载 reservation：stay.reservationId（非空）+ reservation_room 引用
        List<Long> resIds = new ArrayList<>(rrs.stream().map(ReservationRoom::getReservationId).distinct().toList());
        for (Stay s : stays) {
            if (s.getReservationId() != null && !resIds.contains(s.getReservationId())) resIds.add(s.getReservationId());
        }
        Map<Long, Reservation> resById = Map.of();
        if (!resIds.isEmpty()) {
            List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                    .in(Reservation::getId, resIds)
                    .in(Reservation::getStatus, List.of("CONFIRMED", "CHECKED_IN", "CHECKED_OUT"))
                    .le(Reservation::getCheckInDate, to)
                    .gt(Reservation::getCheckOutDate, from));
            resById = reservations.stream().collect(Collectors.toMap(Reservation::getId, r -> r, (a, b) -> a));
        }

        // 线下散客(WALKIN) 渠道：钟点房等无 reservation 的 stay 默认归属
        Channel walkin = channelById.values().stream()
                .filter(c -> "WALKIN".equals(c.getCode())).findFirst().orElse(null);

        // 按日期范围封房（ACTIVE，区间与 [from,to] 重叠）：按 roomId 分组
        List<RoomBlock> blocks = roomBlockMapper.selectList(new LambdaQueryWrapper<RoomBlock>()
                .eq(RoomBlock::getHotelId, hotelId)
                .eq(RoomBlock::getStatus, "ACTIVE")
                .le(RoomBlock::getStartDate, to)
                .ge(RoomBlock::getEndDate, from));
        Map<Long, List<RoomBlock>> blocksByRoom = blocks.stream()
                .collect(Collectors.groupingBy(RoomBlock::getRoomId));

        Map<String, Cell> cells = new LinkedHashMap<>();
        for (Room r : rooms) {
            Stay st = stayByRoom.get(r.getId());
            ReservationRoom rr = rrByRoom.get(r.getId());
            Reservation res = rr == null ? null : resById.get(rr.getReservationId());
            // stay 渠道：有 reservation 取 res.channelId，否则 WALKIN
            Channel stayChannel = null;
            if (st != null) {
                Long cid = st.getReservationId() != null && resById.containsKey(st.getReservationId())
                        ? resById.get(st.getReservationId()).getChannelId()
                        : (walkin == null ? null : walkin.getId());
                stayChannel = cid == null ? null : channelById.get(cid);
            }
            Channel resChannel = res == null || res.getChannelId() == null ? null : channelById.get(res.getChannelId());
            List<RoomBlock> roomBlocks = blocksByRoom.get(r.getId());
            for (String d : dates) {
                LocalDate date = LocalDate.parse(d);
                if (st != null && coversStay(st, date)) {
                    cells.put(r.getId() + ":" + d, cellOfStay(st, r.getId(), d, stayChannel));
                } else if (res != null && coversReservation(res, date)) {
                    cells.put(r.getId() + ":" + d, cellOfReservation(res, r.getId(), d, resChannel));
                } else if (roomBlocks != null) {
                    // 封房填充空格（在住/预订优先展示）；命中即取首条
                    for (RoomBlock b : roomBlocks) {
                        if (coversBlock(b, date)) {
                            cells.put(r.getId() + ":" + d, cellOfBlock(b, r.getId(), d));
                            break;
                        }
                    }
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
     * stay 覆盖某日：
     * - IN（在住）：checkInDate ≤ date ≤ checkOutDate（含退房当日，钟点房 checkOut 同日也覆盖）。
     * - OUT（已退房）：checkInDate ≤ date ＜ checkOutDate（不含退房当日——退房当天房间转脏房，
     *   格子不再显示该宾客，由空格的脏房样式呈现）。
     * 未退房则 checkOutDate 视为 +∞。
     */
    private boolean coversStay(Stay s, LocalDate date) {
        LocalDate ci = s.getCheckInAt() == null ? null : s.getCheckInAt().toLocalDate();
        if (ci == null || date.isBefore(ci)) return false;
        if (s.getCheckOutAt() == null) return true;
        LocalDate co = s.getCheckOutAt().toLocalDate();
        if ("OUT".equals(s.getStatus())) return date.isBefore(co);
        return !date.isAfter(co);
    }

    /** reservation 覆盖某日：checkInDate ≤ date ＜ checkOutDate。 */
    private boolean coversReservation(Reservation res, LocalDate date) {
        if (date.isBefore(res.getCheckInDate())) return false;
        return date.isBefore(res.getCheckOutDate());
    }

    /** 封房覆盖某日：start_date ≤ date ≤ end_date（含首尾两日）。 */
    private boolean coversBlock(RoomBlock b, LocalDate date) {
        return !date.isBefore(b.getStartDate()) && !date.isAfter(b.getEndDate());
    }

    private Cell cellOfStay(Stay s, Long roomId, String date, Channel channel) {
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
        applyChannel(c, channel);
        return c;
    }

    private Cell cellOfReservation(Reservation res, Long roomId, String date, Channel channel) {
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
        applyChannel(c, channel);
        return c;
    }

    private Cell cellOfBlock(RoomBlock b, Long roomId, String date) {
        Cell c = new Cell();
        c.setType("block");
        c.setId(b.getId());
        c.setRoomId(roomId);
        c.setDate(date);
        c.setStatus(b.getStatus());
        c.setGuestName("封房");
        c.setCheckIn(b.getStartDate().toString());
        c.setCheckOut(b.getEndDate().toString());
        c.setHourly(false);
        c.setReason(b.getReason());
        return c;
    }

    private void applyChannel(Cell c, Channel channel) {
        if (channel != null) {
            c.setChannelId(channel.getId());
            c.setChannelName(channel.getName());
            c.setChannelIcon(channel.getIcon());
        }
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

    /**
     * 按日期范围封房（关闭房间）：与全房间 rent_status=CLOSED 的报修不同，
     * 仅在 [start,end]（含首尾）期间关闭该房可售。到期或释放后自动恢复。
     * 拒绝与该房已有 ACTIVE 封房区间重叠的请求。
     */
    @Transactional
    public RoomBlock blockRoom(Long roomId, LocalDate start, LocalDate end, String reason) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        if (start == null || end == null || end.isBefore(start)) {
            throw BusinessException.of(400, "封房日期非法");
        }
        // 同房已有 ACTIVE 封房与 [start,end] 重叠则拒绝
        List<RoomBlock> active = roomBlockMapper.selectList(new LambdaQueryWrapper<RoomBlock>()
                .eq(RoomBlock::getHotelId, hotelId)
                .eq(RoomBlock::getRoomId, roomId)
                .eq(RoomBlock::getStatus, "ACTIVE")
                .le(RoomBlock::getStartDate, end)
                .ge(RoomBlock::getEndDate, start));
        if (!active.isEmpty()) {
            throw BusinessException.of(409, "该房间所选日期已有封房记录");
        }

        RoomBlock b = new RoomBlock();
        b.setHotelId(hotelId);
        b.setRoomId(roomId);
        b.setStartDate(start);
        b.setEndDate(end);
        b.setReason(reason);
        b.setStatus("ACTIVE");
        b.setOperatorId(currentOperator());
        b.setCreatedAt(LocalDateTime.now());
        roomBlockMapper.insert(b);
        return b;
    }

    /** 释放封房：置 RELEASED，对应日期格子即恢复可售。 */
    @Transactional
    public RoomBlock releaseBlock(Long blockId) {
        RoomBlock b = roomBlockMapper.selectById(blockId);
        if (b == null) throw new BusinessException(404, "封房记录不存在");
        if (!"ACTIVE".equals(b.getStatus())) throw BusinessException.of(409, "封房已释放");
        b.setStatus("RELEASED");
        b.setReleasedAt(LocalDateTime.now());
        roomBlockMapper.updateById(b);
        return b;
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
        /** 订单渠道来源 */
        private Long channelId;
        private String channelName;
        /** 渠道图标标识（预留，后续填充） */
        private String channelIcon;
        /** 封房原因（type=block 时填充） */
        private String reason;
    }
}
