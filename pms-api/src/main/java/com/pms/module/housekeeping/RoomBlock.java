package com.pms.module.housekeeping;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 房间按日期范围封房（关闭）：区别于全房间 rent_status=CLOSED 的报修，
 * 封房仅在未来某段时间内关闭该房可售，到期或手动释放后自动恢复。
 * start_date ≤ date ≤ end_date（含首尾两日）视为封房中。
 */
@Data
@TableName("room_block")
public class RoomBlock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    /** ACTIVE 封房中 / RELEASED 已释放 */
    private String status;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime releasedAt;
}
