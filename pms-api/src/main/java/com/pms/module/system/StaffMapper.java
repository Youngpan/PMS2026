package com.pms.module.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StaffMapper extends BaseMapper<Staff> {

    /** 查询员工的角色编码列表。 */
    @Select("SELECT r.code FROM staff_role sr " +
            "JOIN role r ON r.id = sr.role_id " +
            "WHERE sr.staff_id = #{staffId}")
    List<String> selectRoleCodes(@Param("staffId") Long staffId);
}
