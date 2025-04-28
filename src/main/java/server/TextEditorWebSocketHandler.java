package server;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class TextEditorWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("[Server] Client connected: " + session.getId());
        broadcastSystemMessage(session.getId(), "connected");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("[Server] Client disconnected: " + session.getId());
        broadcastSystemMessage(session.getId(), "disconnected");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("[Server] Received message: " + message.getPayload());

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(message); // 수정 필요 없음
            }
        }
    }

    // 시스템 메시지 브로드캐스트용 메소드 추가
    private void broadcastSystemMessage(String clientId, String eventType) throws Exception {
        String systemMessage = "{\"type\":\"system\",\"clientId\":\"" + clientId + "\",\"event\":\"" + eventType + "\"}";

        synchronized (sessions) {
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(systemMessage));
                }
            }
        }
    }
}