package com.pms.module.housekeeping;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("housekeeping_task")
public class HousekeepingTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long roomId;
    /** DAILY / CHECKOUT_CLEAN */
    private String type;
    /** PENDING / DONE */
    private String status;
    private Long assigneeId;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
