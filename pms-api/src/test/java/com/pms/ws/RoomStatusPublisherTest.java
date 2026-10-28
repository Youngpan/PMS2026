package com.pms.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 房态广播纯单元测试（无 Servlet 容器）：
 * 验证 RoomStatusPublisher 序列化 JSON、广播到所有连接、单连接失败不影响其它、无连接时不抛异常。
 */
class RoomStatusPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebSocketSession mockSession(boolean open) {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.isOpen()).thenReturn(open);
        return s;
    }

    @Test
    void publish广播JSON给所有连接() throws Exception {
        RoomStatusWebSocketHandler handler = new RoomStatusWebSocketHandler();
        RoomStatusPublisher publisher = new RoomStatusPublisher(handler, objectMapper);

        WebSocketSession s1 = mockSession(true);
        WebSocketSession s2 = mockSession(true);
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);

        publisher.publish(101L, "101", "OPEN", "DIRTY");

        ArgumentCaptor<TextMessage> cap = ArgumentCaptor.forClass(TextMessage.class);
        verify(s1).sendMessage(cap.capture());
        Map<?, ?> m = objectMapper.readValue(cap.getValue().getPayload(), Map.class);
        assertThat(m.get("type")).isEqualTo("room-status");
        assertThat(m.get("roomId")).isEqualTo(101);
        assertThat(m.get("number")).isEqualTo("101");
        assertThat(m.get("rentStatus")).isEqualTo("OPEN");
        assertThat(m.get("cleanStatus")).isEqualTo("DIRTY");
        assertThat(m.get("ts")).isNotNull();

        verify(s2).sendMessage(any(TextMessage.class));
    }

    @Test
    void 单连接发送失败不影响其它连接() throws Exception {
        RoomStatusWebSocketHandler handler = new RoomStatusWebSocketHandler();
        RoomStatusPublisher publisher = new RoomStatusPublisher(handler, objectMapper);

        WebSocketSession broken = mockSession(true);
        doThrow(new IOException("连接已断")).when(broken).sendMessage(any(TextMessage.class));
        WebSocketSession healthy = mockSession(true);
        handler.afterConnectionEstablished(broken);
        handler.afterConnectionEstablished(healthy);

        // 不应抛异常（broadcast 内部 per-session try/catch）
        publisher.publish(201L, "201", "OPEN", "CLEAN");

        verify(broken).sendMessage(any(TextMessage.class));
        verify(healthy).sendMessage(any(TextMessage.class));
    }

    @Test
    void 无连接时publish不抛异常() {
        RoomStatusWebSocketHandler handler = new RoomStatusWebSocketHandler();
        RoomStatusPublisher publisher = new RoomStatusPublisher(handler, objectMapper);

        org.assertj.core.api.Assertions.assertThatCode(() -> publisher.publish(301L, "301", "CLOSED", "CLEAN"))
                .doesNotThrowAnyException();
    }

    @Test
    void 关闭的连接不会被发送() throws Exception {
        RoomStatusWebSocketHandler handler = new RoomStatusWebSocketHandler();
        RoomStatusPublisher publisher = new RoomStatusPublisher(handler, objectMapper);

        WebSocketSession closed = mockSession(false); // isOpen=false
        handler.afterConnectionEstablished(closed);

        publisher.publish(401L, "401", "ASSIGNED", "CLEAN");

        verify(closed, never()).sendMessage(any(TextMessage.class));
    }
}
