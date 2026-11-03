package com.pms.module.ota;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 渠道订单拉取暂存表（幂等 + 原始快照 + 落单追踪）。
 * 联合唯一键 (channel_order_id, source_type) 保证幂等。
 */
@Data
@TableName("channel_order")
public class ChannelOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long otaAccountId;
    /** 平台: CTRIP/MEITUAN/DOUYIN */
    private String platform;
    /** 渠道侧订单号 */
    private String channelOrderId;
    /** 订单来源类型: Ebooking */
    private String sourceType;
    /** 客人姓名(脱敏) */
    private String guestName;
    /** 客人电话(脱敏) */
    private String guestPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer persons;
    /** 渠道房型名称 */
    private String roomTypeName;
    /** 从渠道侧获取的关联房间ID */
    private Long roomId;
    /** 从渠道侧获取的关联房型ID */
    private Long roomTypeId;
    /** 预订房间数 */
    private Integer roomCount;
    /** 订单总额 */
    private BigDecimal totalAmount;
    private String currency;
    /** 渠道侧订单状态: Receipt/CheckIn/Canceled */
    private String channelStatus;
    /** 原始接口JSON快照 */
    private String rawJson;
    /** RAW未处理/PROCESSED已落单/DUPLICATE重复/FAILED处理失败 */
    private String processStatus;
    /** 落单时间 */
    private LocalDateTime processedAt;
    /** 落单后的百居易预订ID */
    private Long localReservationId;
    /** 处理失败原因 */
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
