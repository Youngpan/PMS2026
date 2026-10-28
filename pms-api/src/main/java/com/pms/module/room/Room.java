package com.pms.module.room;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room")
public class Room {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long roomTypeId;
    private String number;
    private Integer floor;
    /** 可售状态：OPEN打开/CLOSED关闭/ASSIGNED已排房(在住) */
    private String rentStatus;
    /** 卫生状态：CLEAN净房/DIRTY脏房 */
    private String cleanStatus;
    private String remark;
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
