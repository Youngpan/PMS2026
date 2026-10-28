package com.pms.security;

import com.pms.common.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw BusinessException.of(401, "未认证");
        }
        try {
            Claims c = jwtUtil.parse(auth.substring(7));
            StaffContext.set(new StaffContext.StaffPrincipal(
                    Long.parseLong(c.getSubject()),
                    c.get("username", String.class),
                    c.get("realName", String.class)));
            return true;
        } catch (Exception e) {
            throw BusinessException.of(401, "登录已失效");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object h, Exception ex) {
        StaffContext.clear();
    }
}
