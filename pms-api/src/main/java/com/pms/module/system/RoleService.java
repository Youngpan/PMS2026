package com.pms.module.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    public List<Role> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .eq(Role::getHotelId, hotelId));
    }

    public Role create(Role r) {
        r.setId(null);
        r.setHotelId(hotelId);
        roleMapper.insert(r);
        return r;
    }

    public List<Permission> permissionsOfRole(Long roleId) {
        return permissionMapper.selectByRole(roleId);
    }

    /** 重置角色权限（先删后插）。 */
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        Role r = roleMapper.selectById(roleId);
        if (r == null) throw new BusinessException(404, "角色不存在");
        rolePermissionMapper.deleteByRole(roleId);
        if (permissionIds != null) permissionIds.forEach(pid -> rolePermissionMapper.insert(roleId, pid));
    }

    public List<Permission> allPermissions() {
        return permissionMapper.selectList(null);
    }
}
