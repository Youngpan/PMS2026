package com.pms.module.channel;

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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /channels 返回 9 个渠道（美团/携程/飞猪/抖音/高德/美团民宿/途家民宿/线下散客/其他）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class ChannelControllerTest {

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

    @Test
    void 渠道列表返回9项且含全部渠道() throws Exception {
        String token = loginAsAdmin();
        String resp = mvc.perform(get("/channels")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<String> names = JsonPath.read(resp, "$.data[*].name");
        org.assertj.core.api.Assertions.assertThat(names).hasSize(9);
        org.assertj.core.api.Assertions.assertThat(names)
                .containsExactly("线下散客", "携程", "美团", "飞猪", "抖音", "高德", "美团民宿", "途家民宿", "其他");
    }
}
