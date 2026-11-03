package com.pms.module.ota;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OTA 直连账号：携程/美团/抖音 等平台的商家账号。
 * password 字段用 @JsonIgnore 避免返回前端明文。
 */
@Data
@TableName("ota_account")
public class OtaAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    /** OTA 平台: CTRIP携程 / MEITUAN美团 / DOUYIN抖音 */
    private String platform;
    private String accountName;
    /** 密码：只写不读，前端提交时接受，返回时不输出 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /** 商家ID/门店ID */
    private String merchantId;
    /** 开放平台 AppKey（可选，后续 API 对接用） */
    private String appKey;
    /** 开放平台 AppSecret：只写不读 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String appSecret;
    /** DISCONNECTED未连接 / CONNECTED已连接 / ERROR连接异常 */
    private String status;
    private LocalDateTime lastSyncAt;
    private String lastError;
    private Integer enabled;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
