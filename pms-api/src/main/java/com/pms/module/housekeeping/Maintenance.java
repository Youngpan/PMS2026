package com.pms.module.housekeeping;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("maintenance")
public class Maintenance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long roomId;
    private String issue;
    /** OPEN / FIXED */
    private String status;
    private Long reporterId;
    private LocalDateTime createdAt;
    private LocalDateTime fixedAt;
}
