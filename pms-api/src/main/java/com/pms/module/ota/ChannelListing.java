package com.pms.module.ota;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 渠道房源关联表。
 * 百居易房间/房型 ↔ OTA渠道房源的映射关系。
 * 独立房源(inventory_type=INDEPENDENT) → 关联到房间(room_id)
 * 多库存房源(inventory_type=MULTI_STOCK) → 关联到房型(room_type_id)
 */
@Data
@TableName("channel_listing")
public class ChannelListing {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long otaAccountId;
    /** 平台: CTRIP/MEITUAN/DOUYIN */
    private String platform;
    /** 渠道侧房源ID */
    private String channelListingId;
    /** 渠道侧房源名称 */
    private String channelListingName;
    /** 关联类型: ROOM(房间)/ROOM_TYPE(房型) */
    private String targetType;
    /** target_type=ROOM 时的房间ID */
    private Long roomId;
    /** target_type=ROOM_TYPE 时的房型ID */
    private Long roomTypeId;
    /** 库存类型: INDEPENDENT独立房源/MULTI_STOCK多库存 */
    private String inventoryType;
    /** ACTIVE已关联/INACTIVE已解除 */
    private String status;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
