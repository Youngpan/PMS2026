package com.pms.module.room;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("room_type")
public class RoomType {
    @TableId(type = IdType.AUTO)
    private Long id;
    @JsonAlias("hotel_id")
    private Long hotelId;
    private String code;
    private String name;
    @JsonAlias("max_occupancy")
    private Integer maxOccupancy;
    @JsonAlias("bed_type")
    private String bedType;
    private BigDecimal area;
    @JsonAlias("base_rate")
    private BigDecimal baseRate;
    @JsonAlias("rack_rate")
    private BigDecimal rackRate;
}
