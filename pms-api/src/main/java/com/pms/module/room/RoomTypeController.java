package com.pms.module.room;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeMapper roomTypeMapper;

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
        t.setId(id);
        roomTypeMapper.updateById(t);
        return Result.ok(t);
    }
}
