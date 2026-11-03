package com.pms.module.ota;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 冲突订单盒子（订单盒子）。
 * 因同步延迟、房态不一致等原因导致的冲突订单暂存区。
 * PENDING待处理 → RESOLVED已解决 / CANCELLED已取消
 */
@Data
@TableName("conflict_order")
public class ConflictOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long otaAccountId;
    private String platform;
    /** 渠道侧订单号 */
    private String channelOrderId;
    private String guestName;
    private String guestPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Long roomId;
    private Long roomTypeId;
    /** 冲突类型: SYNC_DELAY/OVERLAP/STATUS_MISMATCH/MULTI_STOCK_PUZZLE */
    private String conflictType;
    private String conflictDetail;
    /** PENDING待处理/RESOLVED已解决/CANCELLED已取消 */
    private String status;
    private String resolution;
    private Long handlerId;
    private LocalDateTime handledAt;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
