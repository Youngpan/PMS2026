package com.pms.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.module.room.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 房态变更广播：序列化为 JSON 后推送给所有 WS 客户端。
 * payload 携带可售状态 rentStatus 与卫生状态 cleanStatus 两个独立维度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomStatusPublisher {

    private final RoomStatusWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    public void publish(Room r) {
        publish(r.getId(), r.getNumber(), r.getRentStatus(), r.getCleanStatus());
    }

    public void publish(Long roomId, String number, String rentStatus, String cleanStatus) {
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "room-status");
            msg.put("roomId", roomId);
            msg.put("number", number);
            msg.put("rentStatus", rentStatus);
            msg.put("cleanStatus", cleanStatus);
            msg.put("ts", LocalDateTime.now().toString());
            handler.broadcast(objectMapper.writeValueAsString(msg));
        } catch (Exception e) {
            log.warn("房态广播失败: {}", e.getMessage());
        }
    }
}
