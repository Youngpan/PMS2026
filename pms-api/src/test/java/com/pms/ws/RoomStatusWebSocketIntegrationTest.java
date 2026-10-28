package com.pms.ws;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.module.room.Room;
import com.pms.module.room.RoomMapper;
import com.pms.module.room.RoomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket 全链路集成测试：真 embedded server + 真 WS 握手 + 业务触发广播。
 * 钉死：/api/v1/ws/room-status 端点可升级、RoomService.changeStatus 触发的广播能送达真实客户端。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class RoomStatusWebSocketIntegrationTest {

    @LocalServerPort int port;
    @Autowired RoomService roomService;
    @Autowired RoomMapper roomMapper;
    @Autowired ObjectMapper objectMapper;

    private final BlockingQueue<WebSocketSession> opened = new LinkedBlockingQueue<>();

    /** 捕获服务端推送文本的客户端 handler。 */
    static class CapturingHandler extends TextWebSocketHandler {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.add(message.getPayload());
        }
    }

    private WebSocketSession connect(CapturingHandler handler) throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        return client.doHandshake(handler, "ws://localhost:" + port + "/api/v1/ws/room-status")
                .get(5, TimeUnit.SECONDS);
    }

    @AfterEach
    void cleanup() {
        opened.forEach(s -> { try { s.close(); } catch (Exception ignored) {} });
        opened.clear();
    }

    @Test
    void 房态变更经WS推送到客户端() throws Exception {
        CapturingHandler client = new CapturingHandler();
        WebSocketSession session = connect(client);
        opened.add(session);
        assertThat(session.isOpen()).isTrue();

        Room r101 = roomMapper.selectOne(new LambdaQueryWrapper<Room>().eq(Room::getNumber, "101"));
        roomService.changeStatus(r101.getId(), "VD");

        String payload = client.messages.poll(5, TimeUnit.SECONDS);
        assertThat(payload).isNotNull();
        JsonNode node = objectMapper.readTree(payload);
        assertThat(node.get("type").asText()).isEqualTo("room-status");
        assertThat(node.get("number").asText()).isEqualTo("101");
        assertThat(node.get("status").asText()).isEqualTo("VD");
        assertThat(node.get("roomId").asLong()).isEqualTo(r101.getId());
    }

    @Test
    void 广播送达多个连接的客户端() throws Exception {
        CapturingHandler c1 = new CapturingHandler();
        CapturingHandler c2 = new CapturingHandler();
        opened.add(connect(c1));
        opened.add(connect(c2));

        Room r102 = roomMapper.selectOne(new LambdaQueryWrapper<Room>().eq(Room::getNumber, "102"));
        roomService.changeStatus(r102.getId(), "OOO");

        for (CapturingHandler c : new CapturingHandler[]{c1, c2}) {
            String payload = c.messages.poll(5, TimeUnit.SECONDS);
            assertThat(payload).isNotNull();
            assertThat(objectMapper.readTree(payload).get("number").asText()).isEqualTo("102");
        }
    }
}
