package com.pms.module.report;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("night_audit")
public class NightAudit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private LocalDate auditDate;
    private Integer checkinsCount;
    private Integer checkoutsCount;
    private BigDecimal revenue;
    private Long auditedBy;
    /** DRAFT / CONFIRMED */
    private String status;
    private LocalDateTime createdAt;
}
