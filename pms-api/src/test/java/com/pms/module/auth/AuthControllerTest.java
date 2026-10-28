package com.pms.module.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 鉴权 controller 层：登录发 token、token 校验、未认证拦截。
 * MockMvc 直连 DispatcherServlet，不走 servlet 容器，故路径不含 context-path /api/v1。
 * GlobalExceptionHandler 把 BusinessException 包成 HTTP 200 + body.code，
 * 所以 401 场景断言 body.code 而非 HTTP 状态。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private String loginAsAdmin() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin"));
        String resp = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.data.token");
    }

    @Test
    void 登录成功返回token() throws Exception {
        String token = loginAsAdmin();
        org.assertj.core.api.Assertions.assertThat(token).isNotBlank();
    }

    @Test
    void 密码错误应返回401业务码() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "admin", "password", "wrong"));
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 未带token访问受保护接口应返回401业务码() throws Exception {
        mvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 带合法token访问受保护接口应返回成功() throws Exception {
        String token = loginAsAdmin();
        mvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void 带伪造token访问受保护接口应返回401业务码() throws Exception {
        mvc.perform(get("/reservations")
                        .header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
