package com.pms.module.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("staff")
public class Staff {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private Long departmentId;
    private String username;
    private String passwordHash;
    private String realName;
    private String phone;
    /** 1启用 0停用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
