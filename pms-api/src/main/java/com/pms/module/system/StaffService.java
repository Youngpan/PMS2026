package com.pms.module.system;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.security.JwtUtil;
import com.pms.security.StaffContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffMapper staffMapper;
    private final StaffRoleMapper staffRoleMapper;
    private final JwtUtil jwtUtil;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    @Data
    public static class LoginResult {
        public String token;
        public long staffId;
        public String username;
        public String realName;
        public List<String> roles;
    }

    /** 真实登录：用户名 + bcrypt 校验。 */
    public LoginResult login(String username, String password) {
        if (username == null || password == null) {
            throw new BusinessException(400, "用户名/密码必填");
        }
        Staff s = staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getUsername, username)
                .last("LIMIT 1"));
        if (s == null) throw new BusinessException(401, "用户不存在或密码错误");
        if (s.getStatus() != null && s.getStatus() == 0) {
            throw new BusinessException(403, "账号已停用");
        }
        if (!BCrypt.checkpw(password, s.getPasswordHash())) {
            throw new BusinessException(401, "用户不存在或密码错误");
        }
        List<String> roles = staffMapper.selectRoleCodes(s.getId());
        String token = jwtUtil.generate(s.getId(), s.getUsername(), s.getRealName());
        LoginResult r = new LoginResult();
        r.token = token;
        r.staffId = s.getId();
        r.username = s.getUsername();
        r.realName = s.getRealName();
        r.roles = roles;
        return r;
    }

    public List<Staff> list() {
        return staffMapper.selectList(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getHotelId, hotelId)
                .orderByDesc(Staff::getCreatedAt));
    }

    /** 创建员工，明文密码经 bcrypt 入库。 */
    @Transactional
    public Staff create(Staff s, String plainPassword) {
        if (s.getUsername() == null || plainPassword == null) {
            throw new BusinessException(400, "用户名与密码必填");
        }
        s.setId(null);
        s.setHotelId(hotelId);
        s.setPasswordHash(BCrypt.hashpw(plainPassword));
        if (s.getStatus() == null) s.setStatus(1);
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        staffMapper.insert(s);
        return s;
    }

    /** 修改基本信息（不含密码）。 */
    public Staff update(Staff s) {
        if (s.getId() == null) throw new BusinessException(400, "id 必填");
        s.setPasswordHash(null); // 不覆盖密码
        s.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(s);
        return staffMapper.selectById(s.getId());
    }

    @Transactional
    public void resetPassword(Long staffId, String newPlainPassword) {
        Staff s = staffMapper.selectById(staffId);
        if (s == null) throw new BusinessException(404, "员工不存在");
        s.setPasswordHash(BCrypt.hashpw(newPlainPassword));
        s.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(s);
    }

    @Transactional
    public void toggleStatus(Long staffId, Integer status) {
        Staff s = staffMapper.selectById(staffId);
        if (s == null) throw new BusinessException(404, "员工不存在");
        s.setStatus(status);
        s.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(s);
    }

    @Transactional
    public void assignRoles(Long staffId, List<Long> roleIds) {
        staffRoleMapper.deleteByStaff(staffId);
        if (roleIds != null) roleIds.forEach(rid -> staffRoleMapper.insert(staffId, rid));
    }

    public long currentStaffId() {
        return StaffContext.get() != null ? StaffContext.get().getStaffId() : 0L;
    }
}
