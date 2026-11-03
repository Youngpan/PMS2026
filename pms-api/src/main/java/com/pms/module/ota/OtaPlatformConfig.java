package com.pms.module.ota;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OTA平台配置表。
 * 存储各个OTA平台的字段模板、校验规则等配置。
 */
@Data
@TableName("ota_platform_config")
public class OtaPlatformConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 平台代码: CTRIP/MEITUAN/DOUYIN */
    private String platform;
    /** 配置项名称 */
    private String configName;
    /** 配置值(JSON) */
    private String configValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
