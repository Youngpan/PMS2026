package com.pms.module.ota;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OTA 直连账号（普通直连登录态管理）。
 * 支持携程/抖音/美团酒店的账号密码直连，包含登录态凭证持久化、
 * 保活轮询、掉线检测与自动重连、风控策略配置。
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
    /** 开放平台 AppKey（可选） */
    private String appKey;
    /** 开放平台 AppSecret：只写不读 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String appSecret;

    // ---- 登录态管理 ----
    /** 加密存储的登录态凭证（session / cookie / token） */
    private String sessionToken;
    /** 凭证过期时间 */
    private LocalDateTime sessionExpiresAt;

    // ---- 状态 ----
    /** DISCONNECTED未连接 / CONNECTED已登录 / OFFLINE掉线 / ERROR连接异常 */
    private String status;
    /** 最近掉线时间 */
    private LocalDateTime offlineAt;
    /** 掉线原因 */
    private String offlineReason;
    /** 最近错误信息 */
    private String lastError;

    // ---- 同步 ----
    /** 最近一次同步时间 */
    private LocalDateTime lastSyncAt;

    // ---- 自动重连 ----
    /** 开启自动重连: 1是 / 0否 */
    private Integer autoReconnect;
    /** 当前退避重试次数（指数退避 2^n 分钟） */
    private Integer retryCount;
    /** 下次自动重试时间 */
    private LocalDateTime nextRetryAt;

    // ---- 保活配置 ----
    /** 保活轮询间隔（秒），默认 300（5 分钟） */
    private Integer keepaliveInterval;

    // ---- 风控配置 ----
    /** 风控配置 JSON：User-Agent / 设备指纹 / 出口IP */
    private String serverConfig;

    // ---- 通用 ----
    private Integer enabled;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
