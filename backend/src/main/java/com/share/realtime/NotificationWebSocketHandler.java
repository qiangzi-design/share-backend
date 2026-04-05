package com.share.realtime;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final NotificationWebSocketSessionManager sessionManager;

    public NotificationWebSocketHandler(NotificationWebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = parseUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }
        sessionManager.addSession(userId, session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Currently no client commands are needed.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = parseUserId(session);
        if (userId != null) {
            sessionManager.removeSession(userId, session);
        }
    }

    private Long parseUserId(WebSocketSession session) {
        Object value = session.getAttributes().get("userId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
