package com.pms.module.room;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.Result;
import com.pms.ota.OtaEvent;
import com.pms.ota.OtaEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeMapper roomTypeMapper;
    private final OtaEventPublisher otaEventPublisher;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    @GetMapping
    public Result<List<RoomType>> list() {
        return Result.ok(roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                .eq(RoomType::getHotelId, hotelId)
                .orderByAsc(RoomType::getCode)));
    }

    @PostMapping
    public Result<RoomType> create(@RequestBody RoomType t) {
        t.setId(null);
        t.setHotelId(hotelId);
        roomTypeMapper.insert(t);
        return Result.ok(t);
    }

    @PutMapping("/{id}")
    public Result<RoomType> update(@PathVariable Long id, @RequestBody RoomType t) {
        RoomType old = roomTypeMapper.selectById(id);
        t.setId(id);
        roomTypeMapper.updateById(t);
        // 房价变更 → 广播给所有 OTA（ARI rates 推送）
        if (old != null && t.getBaseRate() != null && !t.getBaseRate().equals(old.getBaseRate())) {
            otaEventPublisher.publish(OtaEvent.RefType.ROOM_RATE, id, "RATE_CHANGED", null,
                    Map.of("roomTypeId", id, "baseRate", t.getBaseRate().toString(),
                            "oldBaseRate", old.getBaseRate() == null ? "" : old.getBaseRate().toString()));
        }
        return Result.ok(t);
    }
}
