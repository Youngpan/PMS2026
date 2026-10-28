package com.pms.module.folio;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("folio_item")
public class FolioItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long folioId;
    /** CHARGE/DEPOSIT/PAYMENT */
    private String type;
    private String category;
    /** 消费为正，押金/支付为负 */
    private BigDecimal amount;
    private Long refFolioId;
    private String remark;
    private Long operatorId;
    private LocalDateTime createdAt;
}
