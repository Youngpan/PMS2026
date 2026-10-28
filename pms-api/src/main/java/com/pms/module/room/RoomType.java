package com.pms.module.room;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("room_type")
public class RoomType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private String code;
    private String name;
    private Integer maxOccupancy;
    private String bedType;
    private BigDecimal area;
    private BigDecimal baseRate;
    private BigDecimal rackRate;
}
