package com.pms.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.module.system.Staff;
import com.pms.module.system.StaffMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动初始化：若 admin 账号仍为占位密码哈希，则用 bcrypt 设置默认密码 admin。
 * 生产环境请通过 /staff/{id}/password 修改。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StaffMapper staffMapper;

    @Value("${pms.default-admin.password:admin}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        Staff admin = staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getUsername, "admin")
                .last("LIMIT 1"));
        if (admin != null && (admin.getPasswordHash() == null
                || admin.getPasswordHash().isEmpty()
                || admin.getPasswordHash().startsWith("bcrypt-placeholder"))) {
            admin.setPasswordHash(BCrypt.hashpw(defaultAdminPassword));
            admin.setUpdatedAt(LocalDateTime.now());
            staffMapper.updateById(admin);
            log.info("已为 admin 设置默认密码（建议立即修改）");
        }
    }
}
