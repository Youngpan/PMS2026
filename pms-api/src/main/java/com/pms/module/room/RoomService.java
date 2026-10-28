package com.pms.module.room;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.BusinessException;
import com.pms.common.PageResult;
import com.pms.ws.RoomStatusPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;
    private final RoomStatusPublisher publisher;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    public PageResult<Room> page(long pageNo, long pageSize, String status, Long roomTypeId) {
        Page<Room> p = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Room> w = new LambdaQueryWrapper<Room>()
                .eq(Room::getHotelId, hotelId)
                .eq(status != null, Room::getStatus, status)
                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                .orderByAsc(Room::getFloor, Room::getNumber);
        roomMapper.selectPage(p, w);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    public Room create(Room r) {
        r.setId(null);
        r.setHotelId(hotelId);
        if (r.getStatus() == null) r.setStatus("VC");
        roomMapper.insert(r);
        return r;
    }

    /** 切换房态，带状态校验。 */
    public Room changeStatus(Long roomId, String target) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        r.setStatus(target);
        roomMapper.updateById(r);
        publisher.publish(r.getId(), r.getNumber(), target);
        return r;
    }

    /** 由房号查可用（VC）房，供入住选择。 */
    public java.util.List<Room> vacantRooms(Long roomTypeId) {
        return roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getHotelId, hotelId)
                .eq(Room::getStatus, "VC")
                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                .orderByAsc(Room::getNumber));
    }

    /** 占用房间（入住时调用），乐观更新。 */
    public void occupy(Long roomId) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        if (!"VC".equals(r.getStatus())) {
            throw BusinessException.of(409, "房间当前状态不可入住: " + r.getStatus());
        }
        r.setStatus("OCC");
        roomMapper.updateById(r);
        publisher.publish(r.getId(), r.getNumber(), "OCC");
    }

    /** 释放房间（退房时调用）→ 脏房 VD。 */
    public void release(Long roomId) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) return;
        r.setStatus("VD");
        roomMapper.updateById(r);
        publisher.publish(r.getId(), r.getNumber(), "VD");
    }
}
