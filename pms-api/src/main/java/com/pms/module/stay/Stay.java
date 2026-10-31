package com.pms.module.stay;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stay")
public class Stay {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long reservationId;
    private Long guestId;
    private String guestName;
    private String guestPhone;
    private Long roomId;
    private Long roomTypeId;
    private BigDecimal rate;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    /** IN/OUT */
    private String status;
    /** STAY 普通入住 / HOURLY 钟点房 */
    private String type;
    /** 钟点房时长（小时） */
    private Integer hours;
    private Long operatorId;

    /** 非持久化字段：房间号，在 inHouse/front 查询时由 service 填充。 */
    @TableField(exist = false)
    private String roomNumber;
}
