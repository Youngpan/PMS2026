package com.pms.module.system;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 员工角色关联（复合主键，不走 BaseMapper）。
 */
@Mapper
public interface StaffRoleMapper {

    @Insert("INSERT INTO staff_role(staff_id, role_id) VALUES(#{staffId}, #{roleId})")
    int insert(@Param("staffId") Long staffId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM staff_role WHERE staff_id = #{staffId}")
    int deleteByStaff(@Param("staffId") Long staffId);
}
