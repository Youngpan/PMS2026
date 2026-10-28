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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 幂等切面：POST /reservations 带 Idempotency-Key，
 * 相同 key 命中缓存返回同一 id；不同 key 走真实逻辑产生不同 id。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class IdempotencyAspectTest {

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

    private Map<String, Object> validReservationBody() {
        return Map.of(
                "guestName", "IdempotencyTest",
                "guestPhone", "13800000002",
                "checkInDate", "2026-10-27",
                "checkOutDate", "2026-10-28",
                "channelId", 1,
                "rooms", List.of(Map.of("roomTypeId", 1, "roomCount", 1, "rate", 280)));
    }

    private Long createWithKey(String token, String idempotencyKey) throws Exception {
        String body = objectMapper.writeValueAsString(validReservationBody());
        String resp = mvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(resp, "$.data.reservation.id");
        return id.longValue();
    }

    @Test
    void 相同幂等键返回同一预订id() throws Exception {
        String token = loginAsAdmin();
        String key = "test-" + UUID.randomUUID();
        Long first = createWithKey(token, key);
        Long second = createWithKey(token, key);
        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);
    }

    @Test
    void 不同幂等键产生不同预订id() throws Exception {
        String token = loginAsAdmin();
        Long first = createWithKey(token, "key-A-" + UUID.randomUUID());
        Long second = createWithKey(token, "key-B-" + UUID.randomUUID());
        org.assertj.core.api.Assertions.assertThat(second).isNotEqualTo(first);
    }

    @Test
    void 不带幂等键每次都走真实逻辑() throws Exception {
        String token = loginAsAdmin();
        // 不带 Idempotency-Key：切面直接 proceed，两次产生不同 id
        String body = objectMapper.writeValueAsString(validReservationBody());
        String r1 = mvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String r2 = mvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Long id1 = ((Number) JsonPath.read(r1, "$.data.reservation.id")).longValue();
        Long id2 = ((Number) JsonPath.read(r2, "$.data.reservation.id")).longValue();
        org.assertj.core.api.Assertions.assertThat(id2).isNotEqualTo(id1);
    }
}
