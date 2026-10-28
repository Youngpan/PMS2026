package com.pms.module.system;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色权限关联（复合主键，不走 BaseMapper）。
 */
@Mapper
public interface RolePermissionMapper {

    @Insert("INSERT INTO role_permission(role_id, permission_id) VALUES(#{roleId}, #{permissionId})")
    int insert(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    int deleteByRole(@Param("roleId") Long roleId);
}
