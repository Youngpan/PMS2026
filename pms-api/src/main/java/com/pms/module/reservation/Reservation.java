package com.pms.module.reservation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private String resNo;
    private Long guestId;
    private String guestName;
    private String guestPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Long channelId;
    /** PENDING/CONFIRMED/CHECKED_IN/NO_SHOW/CANCELLED/CHECKED_OUT */
    private String status;
    private Long bookerId;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
