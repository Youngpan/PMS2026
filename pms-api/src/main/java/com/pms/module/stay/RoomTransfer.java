package com.pms.module.stay;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_transfer")
public class RoomTransfer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stayId;
    private Long fromRoomId;
    private Long toRoomId;
    private String reason;
    private Long operatorId;
    private LocalDateTime createdAt;
}
