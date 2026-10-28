package com.pms.module.folio;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long folioId;
    /** CASH/CARD/WECHAT/ALIPAY/AR */
    private String method;
    private BigDecimal amount;
    private Long operatorId;
    private LocalDateTime paidAt;
}
