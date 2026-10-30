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
        if (r.getRentStatus() == null) r.setRentStatus("OPEN");
        if (r.getCleanStatus() == null) r.setCleanStatus("CLEAN");
        roomMapper.insert(r);
        return r;
    }

    /** 切换可售状态：OPEN 打开 / CLOSED 关闭。 */
    public Room setRentStatus(Long roomId, String rentStatus) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        r.setRentStatus(rentStatus);
        roomMapper.updateById(r);
        publisher.publish(r);
        return r;
    }

    /** 切换卫生状态：CLEAN 净 / DIRTY 脏。 */
    public Room setCleanStatus(Long roomId, String cleanStatus) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) throw new BusinessException(404, "房号不存在");
        r.setCleanStatus(cleanStatus);
        roomMapper.updateById(r);
        publisher.publish(r);
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
