package com.pms.module.ota;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渠道房源产品表。
 * 记录每个 OTA 渠道上的房源产品信息、配置、政策等。
 */
@Data
@TableName("channel_product")
public class ChannelProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long otaAccountId;
    /** 平台: CTRIP/MEITUAN/DOUYIN */
    private String platform;

    // ---- 基础信息 ----
    /** 渠道侧房源ID */
    private String channelListingId;
    /** 渠道房源名称 */
    private String listingName;
    /** 关联类型: ROOM(房间)/ROOM_TYPE(房型) */
    private String targetType;
    /** target_type=ROOM 时关联的房间ID */
    private Long roomId;
    /** target_type=ROOM_TYPE 时关联的房型ID */
    private Long roomTypeId;
    /** 库存类型: INDEPENDENT/MULTI_STOCK */
    private String inventoryType;

    // ---- 房源状态 ----
    /** 在售ON_SALE/停售OFF_SALE */
    private String productStatus;
    /** 是否直连房: 1是/0否 */
    private Integer isDirect;

    // ---- 价格与发票 ----
    /** 含早: 1是/0否 */
    private Integer breakfast;
    /** 早餐费(元/份) */
    private BigDecimal breakfastFee;
    /** 发票类型: DEFAULT普通发票/SPECIAL专用发票 */
    private String invoiceType;
    /** 发票税点(%) */
    private BigDecimal invoiceTaxRate;

    // ---- 入住与退改 ----
    /** 入住方式: FRONT_DESK前台/SELF自助/CODE二维码 */
    private String checkinMethod;
    /** 房费支付方式: ONLINE线上/FRONT前台/CREDIT卡到付 */
    private String paymentMethod;
    /** 担保政策 */
    private String guaranteePolicy;
    /** 取消政策 */
    private String cancelPolicy;

    // ---- 容量与设施 ----
    private Integer ns;
    private Integer nr;
    /** 容纳人数 */
    private Integer capacity;
    /** 加床: 1可加/0不可加 */
    private Integer extraBed;
    /** 加人: 1可加/0不可加 */
    private Integer extraPerson;
    /** 儿童政策 */
    private String childPolicy;
    /** 宠物政策 */
    private String petPolicy;

    // ---- 客房配置 ----
    /** 床型: 大床/双床/单人床 */
    private String bedType;
    /** 楼层 */
    private String floor;
    /** 窗户: 有窗/无窗/部分有窗 */
    @TableField("`window`")
    private String window;
    /** 面积(㎡) */
    private BigDecimal area;

    // ---- 其他 ----
    private String status;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
