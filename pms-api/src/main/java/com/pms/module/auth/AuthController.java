package com.pms.module.auth;

import com.pms.common.Result;
import com.pms.module.system.StaffService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final StaffService staffService;

    @Data
    public static class LoginReq {
        private String username;
        private String password;
    }

    /** 登录：用户名 + bcrypt 校验，返回 JWT 与角色。 */
    @PostMapping("/login")
    public Result<StaffService.LoginResult> login(@RequestBody LoginReq req) {
        return Result.ok(staffService.login(req.getUsername(), req.getPassword()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，客户端丢弃 token 即可；如需吊销可加 Redis 黑名单（后续迭代）
        return Result.ok();
    }
}
