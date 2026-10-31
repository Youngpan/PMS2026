package com.pms.ota;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口调用日志：记录每次对外（OTA/渠道管理器）的事件待推送/已推送记录。
 * channel=渠道 code（如 CTRIP）；direction=in(接单)/out(推送状态·ARI)；status=PENDING/SUCCESS/FAILED。
 */
@Data
@TableName("interface_log")
public class InterfaceLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String channel;
    private String direction;
    private String payload;
    private String status;
    private LocalDateTime createdAt;
}
