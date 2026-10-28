package com.pms.module.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@TableName("hotel")
public class Hotel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String address;
    private String phone;
    private String defaultCurrency;
    private BigDecimal serviceFeeRate;
    private BigDecimal defaultDeposit;
    private LocalTime checkoutTime;
}
