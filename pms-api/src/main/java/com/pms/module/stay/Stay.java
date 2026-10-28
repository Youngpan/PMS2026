package com.pms.module.stay;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private Long roomId;
    private Long roomTypeId;
    private BigDecimal rate;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    /** IN/OUT */
    private String status;
    private Long operatorId;
}
