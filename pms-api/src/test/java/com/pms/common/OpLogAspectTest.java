package com.pms.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.pms.module.system.OperationLog;
import com.pms.module.system.OperationLogMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 审计切面：@OpLog 标注的写操作落库 operation_log，
 * 行的 module/action/staffId 与注解及当前登录用户一致。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class OpLogAspectTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OperationLogMapper operationLogMapper;

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
                "guestName", "OpLogTest",
                "guestPhone", "13800000003",
                "checkInDate", "2026-10-27",
                "checkOutDate", "2026-10-28",
                "channelId", 1,
                "rooms", List.of(Map.of("roomTypeId", 1, "roomCount", 1, "rate", 280)));
    }

    @Test
    void 新建预订落库operation_log且模块动作正确() throws Exception {
        String token = loginAsAdmin();
        long staffId = ((Number) JsonPath.read(mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin"))))
                .andReturn().getResponse().getContentAsString(), "$.data.staffId")).longValue();

        long before = operationLogMapper.selectCount(null);

        String body = objectMapper.writeValueAsString(validReservationBody());
        mvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        long after = operationLogMapper.selectCount(null);
        assertThat(after).isEqualTo(before + 1);

        List<OperationLog> latest = operationLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OperationLog>()
                        .orderByDesc(OperationLog::getId)
                        .last("LIMIT 1"));
        assertThat(latest).hasSize(1);
        OperationLog row = latest.get(0);
        assertThat(row.getModule()).isEqualTo("M3");
        assertThat(row.getAction()).isEqualTo("新建预订");
        assertThat(row.getStaffId()).isEqualTo(staffId);
        assertThat(row.getDetail()).contains("OpLogTest");
    }

    @Test
    void 业务异常时不落库operation_log() throws Exception {
        String token = loginAsAdmin();
        long before = operationLogMapper.selectCount(null);

        // 非法日期：service 抛 BusinessException，pjp.proceed() 抛异常 → 切面不执行落库分支
        String body = objectMapper.writeValueAsString(Map.of(
                "guestName", "OpLogFail",
                "guestPhone", "13800000004",
                "checkInDate", "2026-10-27",
                "checkOutDate", "2026-10-27", // 离店==入住，非法
                "channelId", 1,
                "rooms", List.of(Map.of("roomTypeId", 1, "roomCount", 1, "rate", 280))));
        mvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(400));

        long after = operationLogMapper.selectCount(null);
        assertThat(after).isEqualTo(before); // 无新增
    }
}
