package com.pms.module.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /** 查询角色已分配的权限。 */
    @Select("SELECT p.* FROM role_permission rp " +
            "JOIN permission p ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    List<Permission> selectByRole(@Param("roleId") Long roleId);

    /** 查询员工全部权限（经角色聚合）。 */
    @Select("SELECT DISTINCT p.* FROM staff_role sr " +
            "JOIN role_permission rp ON rp.role_id = sr.role_id " +
            "JOIN permission p ON p.id = rp.permission_id " +
            "WHERE sr.staff_id = #{staffId}")
    List<Permission> selectByStaff(@Param("staffId") Long staffId);
}
