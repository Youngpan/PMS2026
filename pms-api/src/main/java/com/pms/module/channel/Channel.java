package com.pms.module.channel;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 渠道来源：美团/携程/飞猪/抖音/高德/美团民宿/途家民宿/线下散客/其他。
 * icon 预留字段，后续填充具体图标标识。
 */
@Data
@TableName("channel")
public class Channel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    /** 1=OTA，0=非OTA（线下散客/其他） */
    private Integer isOta;
    /** 预留图标标识，后续填充 */
    private String icon;
}
