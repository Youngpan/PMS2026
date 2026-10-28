package com.pms.module.housekeeping;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.module.room.Room;
import com.pms.module.room.RoomMapper;
import com.pms.security.StaffContext;
import com.pms.ws.RoomStatusPublisher;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        if (r != null && "VD".equals(r.getStatus())) {
            r.setStatus("VC");
            roomMapper.updateById(r);
            publisher.publish(r.getId(), r.getNumber(), "VC");
        }
        return t;
    }

    /** 报修：建维修单，房态置 OOO。 */
    @Transactional
    public Maintenance reportMaintenance(Long roomId, String issue) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        r.setStatus("OOO");
        roomMapper.updateById(r);
        publisher.publish(r.getId(), r.getNumber(), "OOO");

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
        if (r != null && "OOO".equals(r.getStatus())) {
            r.setStatus("VC");
            roomMapper.updateById(r);
            publisher.publish(r.getId(), r.getNumber(), "VC");
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
}
