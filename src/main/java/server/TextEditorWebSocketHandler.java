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
        String userId = getUserId(session);
        System.out.println("[Server] Client connected: " + userId);
        broadcastSystemMessage(userId, "connected");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        String userId = getUserId(session);
        System.out.println("[Server] Client disconnected: " + userId);
        broadcastSystemMessage(userId, "disconnected");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("[Server] Received message: " + message.getPayload());

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(message); // 메시지를 그대로 모든 세션에 전달
            }
        }
    }

    private void broadcastSystemMessage(String userId, String event) throws Exception {
        String systemMessage = String.format("{\"type\":\"system\",\"clientId\":\"%s\",\"event\":\"%s\"}", userId, event);

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(systemMessage));
            }
        }
    }

    private String getUserId(WebSocketSession session) {
        String query = session.getUri().getQuery(); // e.g., userId=abc123
        if (query != null && query.startsWith("userId=")) {
            return query.substring("userId=".length());
        }
        return "unknown";
    }
}
