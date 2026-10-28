package com.pms.module.folio;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("folio")
public class Folio {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long stayId;
    private Long reservationId;
    private Long guestId;
    /** 正=应收 */
    private BigDecimal balance;
    /** OPEN/CLOSED */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
