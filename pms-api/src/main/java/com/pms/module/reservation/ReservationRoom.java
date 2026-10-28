package com.pms.module.reservation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("reservation_room")
public class ReservationRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reservationId;
    private Long roomTypeId;
    private Integer roomCount;
    private BigDecimal rate;
    /** 排房后绑定的具体房号 */
    private Long assignedRoomId;
}
