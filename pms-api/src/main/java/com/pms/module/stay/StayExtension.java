package com.pms.module.stay;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stay_extension")
public class StayExtension {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stayId;
    private LocalDate extendToDate;
    private String remark;
    private Long operatorId;
    private LocalDateTime createdAt;
}
