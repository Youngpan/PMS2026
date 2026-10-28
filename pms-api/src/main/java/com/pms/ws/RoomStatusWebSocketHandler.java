package com.pms.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房态实时推送：维护连接集合，收到房态变更时广播 JSON。
 */
@Slf4j
@Component
public class RoomStatusWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WS 连接: {} (共 {})", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WS 关闭: {} (剩 {})", session.getId(), sessions.size());
    }

    /** 向所有连接广播房态变更。 */
    public void broadcast(String message) {
        TextMessage text = new TextMessage(message);
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) s.sendMessage(text);
            } catch (IOException e) {
                log.warn("WS 推送失败 {}: {}", s.getId(), e.getMessage());
            }
        }
    }
}
