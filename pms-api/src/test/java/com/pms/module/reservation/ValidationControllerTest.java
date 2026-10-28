package com.pms.module.reservation;

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

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 层输入校验：非法请求体经 GlobalExceptionHandler 包成 body.code=400。
 * （本项目用手工校验抛 BusinessException(code=400)，非 Bean Validation；
 *  GlobalExceptionHandler 对 BusinessException 返回 HTTP 200 + body.code。）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class ValidationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private String loginAsAdmin() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin"));
        String resp = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.data.token");
    }

    private void postReservation(String token, Map<String, Object> body, int expectedCode) throws Exception {
        mvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    @Test
    void 离店不晚于入住应返回400业务码() throws Exception {
        String token = loginAsAdmin();
        postReservation(token, Map.of(
                "guestName", "V1",
                "guestPhone", "13800000010",
                "checkInDate", "2026-10-27",
                "checkOutDate", "2026-10-27",
                "channelId", 1,
                "rooms", List.of(Map.of("roomTypeId", 1, "roomCount", 1, "rate", 280))
        ), 400);
    }

    @Test
    void 房型明细为空应返回400业务码() throws Exception {
        String token = loginAsAdmin();
        postReservation(token, Map.of(
                "guestName", "V2",
                "guestPhone", "13800000011",
                "checkInDate", "2026-10-27",
                "checkOutDate", "2026-10-28",
                "channelId", 1,
                "rooms", List.of()
        ), 400);
    }

    @Test
    void 合法请求应返回成功码0() throws Exception {
        String token = loginAsAdmin();
        postReservation(token, Map.of(
                "guestName", "V3",
                "guestPhone", "13800000012",
                "checkInDate", "2026-10-27",
                "checkOutDate", "2026-10-28",
                "channelId", 1,
                "rooms", List.of(Map.of("roomTypeId", 1, "roomCount", 1, "rate", 280))
        ), 0);
    }
}
