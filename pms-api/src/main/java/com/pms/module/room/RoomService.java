package com.pms.module.room;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.BusinessException;
import com.pms.common.PageResult;
import com.pms.ota.OtaEvent;
import com.pms.ota.OtaEventPublisher;
import com.pms.ws.RoomStatusPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomStatusPublisher publisher;
    private final OtaEventPublisher otaEventPublisher;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    public PageResult<Room> page(long pageNo, long pageSize, String rentStatus, String cleanStatus, Long roomTypeId) {
        Page<Room> p = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Room> w = new LambdaQueryWrapper<Room>()
                .eq(Room::getHotelId, hotelId)
                .eq(rentStatus != null && !rentStatus.isEmpty(), Room::getRentStatus, rentStatus)
                .eq(cleanStatus != null && !cleanStatus.isEmpty(), Room::getCleanStatus, cleanStatus)
                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                .orderByAsc(Room::getFloor, Room::getNumber);
        roomMapper.selectPage(p, w);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    public Room create(Room r) {
        r.setId(null);
        r.setHotelId(hotelId);
        requireRoomType(r.getRoomTypeId());
        if (r.getNumber() == null || r.getNumber().isBlank()) {
            throw BusinessException.of(400, "房号不能为空");
        }
        r.setNumber(r.getNumber().trim());
        assertNumberFree(r.getNumber(), null);
        if (r.getRentStatus() == null) r.setRentStatus("OPEN");
        if (r.getCleanStatus() == null) r.setCleanStatus("CLEAN");
        if (r.getFloor() == null) r.setFloor(inferFloor(r.getNumber()));
        roomMapper.insert(r);
        publisher.publish(r);
        otaEventPublisher.publish(OtaEvent.RefType.ROOM_AVAILABILITY, r.getId(), "CREATED", null,
                Map.of("roomId", r.getId(), "number", r.getNumber(), "roomTypeId", r.getRoomTypeId()));
        return r;
    }

    /** 修改房间基础信息：房号 / 楼层 / 房型 / 备注（字段为 null 表示不修改）。 */
    public Room update(Long roomId, Room patch) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");

        if (patch.getNumber() != null) {
            String number = patch.getNumber().trim();
            if (number.isEmpty()) throw BusinessException.of(400, "房号不能为空");
            if (!number.equals(r.getNumber())) {
                assertNumberFree(number, roomId);
                r.setNumber(number);
            }
        }
        if (patch.getFloor() != null) r.setFloor(patch.getFloor());
        if (patch.getRoomTypeId() != null && !patch.getRoomTypeId().equals(r.getRoomTypeId())) {
            requireRoomType(patch.getRoomTypeId());
            r.setRoomTypeId(patch.getRoomTypeId());
        }
        if (patch.getRemark() != null) r.setRemark(patch.getRemark());

        roomMapper.updateById(r);
        publisher.publish(r);
        otaEventPublisher.publish(OtaEvent.RefType.ROOM_AVAILABILITY, roomId, "UPDATED", null,
                Map.of("roomId", roomId, "number", r.getNumber(), "roomTypeId", r.getRoomTypeId()));
        return r;
    }

    /** 批量新增房号：整批校验（房型存在、房号非空且不重复）后一次性写入。 */
    public List<Room> batchCreate(RoomBatchRequest req) {
        if (req == null) throw BusinessException.of(400, "请求体为空");
        requireRoomType(req.roomTypeId());
        if (req.numbers() == null || req.numbers().isEmpty()) {
            throw BusinessException.of(400, "请至少填写一个房号");
        }

        List<String> numbers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String raw : req.numbers()) {
            String number = raw == null ? "" : raw.trim();
            if (number.isEmpty()) throw BusinessException.of(400, "房号不能为空");
            if (!seen.add(number)) throw BusinessException.of(409, "房号重复: " + number);
            numbers.add(number);
        }

        List<Room> exists = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getHotelId, hotelId)
                .in(Room::getNumber, numbers));
        if (!exists.isEmpty()) {
            String dup = exists.stream().map(Room::getNumber).sorted().reduce((a, b) -> a + ", " + b).orElse("");
            throw BusinessException.of(409, "房号已存在: " + dup);
        }

        List<Room> created = new ArrayList<>(numbers.size());
        for (String number : numbers) {
            Room r = new Room();
            r.setHotelId(hotelId);
            r.setRoomTypeId(req.roomTypeId());
            r.setNumber(number);
            r.setFloor(req.floor() != null ? req.floor() : inferFloor(number));
            r.setRentStatus("OPEN");
            r.setCleanStatus("CLEAN");
            r.setRemark(req.remark());
            roomMapper.insert(r);
            created.add(r);
        }

        otaEventPublisher.publish(OtaEvent.RefType.ROOM_AVAILABILITY, req.roomTypeId(), "BATCH_CREATED", null,
                Map.of("roomTypeId", req.roomTypeId(), "count", created.size()));
        return created;
    }

    /** 房型必须存在，否则会写出孤儿房号。 */
    private void requireRoomType(Long roomTypeId) {
        if (roomTypeId == null) throw BusinessException.of(400, "请选择房型");
        if (roomTypeMapper.selectById(roomTypeId) == null) {
            throw BusinessException.of(400, "房型不存在: " + roomTypeId);
        }
    }

    /** 房号在本酒店内唯一。 */
    private void assertNumberFree(String number, Long excludeRoomId) {
        Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getHotelId, hotelId)
                .eq(Room::getNumber, number)
                .ne(excludeRoomId != null, Room::getId, excludeRoomId));
        if (count != null && count > 0) {
            throw BusinessException.of(409, "房号已存在: " + number);
        }
    }

    /** 房号首位视为楼层，如 1208 → 12。无法推断时返回 null。 */
    private Integer inferFloor(String number) {
        if (number == null || number.length() < 2) return null;
        int len = 0;
        while (len < number.length() && Character.isDigit(number.charAt(len))) len++;
        if (len < 2) return null;
        try {
            return Integer.parseInt(number.substring(0, len - 2));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 切换可售状态：OPEN 打开 / CLOSED 关闭。 */
    public Room setRentStatus(Long roomId, String rentStatus) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        r.setRentStatus(rentStatus);
        roomMapper.updateById(r);
        publisher.publish(r);
        otaEventPublisher.publish(OtaEvent.RefType.ROOM_AVAILABILITY, roomId, "RENT_CHANGED", null,
                Map.of("roomId", roomId, "rentStatus", rentStatus, "roomTypeId", r.getRoomTypeId()));
        return r;
    }

    /** 切换卫生状态：CLEAN 净 / DIRTY 脏。 */
    public Room setCleanStatus(Long roomId, String cleanStatus) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        r.setCleanStatus(cleanStatus);
        roomMapper.updateById(r);
        publisher.publish(r);
        otaEventPublisher.publish(OtaEvent.RefType.ROOM_AVAILABILITY, roomId, "CLEAN_CHANGED", null,
                Map.of("roomId", roomId, "cleanStatus", cleanStatus, "roomTypeId", r.getRoomTypeId()));
        return r;
    }

    /** 更新房间备注。 */
    public Room setRemark(Long roomId, String remark) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        r.setRemark(remark);
        roomMapper.updateById(r);
        return r;
    }

    /** 由房号查可用（打开+净房）房，供入住选择。 */
    public java.util.List<Room> vacantRooms(Long roomTypeId) {
        return roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getHotelId, hotelId)
                .eq(Room::getRentStatus, "OPEN")
                .eq(Room::getCleanStatus, "CLEAN")
                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                .orderByAsc(Room::getNumber));
    }

    /** 占用房间（入住/换房时调用）：须为 打开+净房 → 已排房。返回占用后的房。 */
    public Room occupy(Long roomId) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        if (!"OPEN".equals(r.getRentStatus()) || !"CLEAN".equals(r.getCleanStatus())) {
            throw BusinessException.of(409, "房间当前状态不可入住: rent=" + r.getRentStatus() + ", clean=" + r.getCleanStatus());
        }
        r.setRentStatus("ASSIGNED");
        roomMapper.updateById(r);
        publisher.publish(r);
        return r;
    }

    /** 释放房间（退房时调用）：可售恢复打开，卫生置脏房。 */
    public void release(Long roomId) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) return;
        r.setRentStatus("OPEN");
        r.setCleanStatus("DIRTY");
        roomMapper.updateById(r);
        publisher.publish(r);
    }

    /** 撤销入住时恢复房态：回到 打开+净房（与退房置脏不同，因其未实际占用过夜）。 */
    public Room revertOccupy(Long roomId) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        r.setRentStatus("OPEN");
        r.setCleanStatus("CLEAN");
        roomMapper.updateById(r);
        publisher.publish(r);
        return r;
    }
}
