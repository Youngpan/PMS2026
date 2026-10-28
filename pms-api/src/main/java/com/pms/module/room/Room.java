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
    /** VC净房 VD脏房 OCC在住 OOO维修 BLOCK预留 */
    private String status;
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
