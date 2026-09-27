package com.reme.re_me.websocket;

import com.reme.re_me.dto.MessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import com.reme.re_me.service.PublicUserIdService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageWebSocketHandler.class);
    private static final String USER_ID_ATTRIBUTE = "userId";

    private final ObjectMapper objectMapper;
    private final PublicUserIdService publicUserIdService;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public MessageWebSocketHandler(ObjectMapper objectMapper, PublicUserIdService publicUserIdService) {
        this.objectMapper = objectMapper;
        this.publicUserIdService = publicUserIdService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String userIdParameter = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("userId");
        try {
            if (userIdParameter == null) {
                throw new NumberFormatException("Missing userId");
            }
            Long userId = publicUserIdService.resolveInternalId(userIdParameter);
            session.getAttributes().put(USER_ID_ATTRIBUTE, userId);
            sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        } catch (IllegalArgumentException exception) {
            logger.warn("Rejected message WebSocket connection with invalid userId");
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.warn("Message WebSocket transport failed", exception);
        removeSession(session);
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException closeException) {
            logger.warn("Failed to close message WebSocket session", closeException);
        }
    }

    public void sendToUser(Long userId, MessageDto message) {
        sendEventToUser(userId, message, "message");
    }

    public void sendConversationDeleted(Long userId, String partnerEmail, boolean forceClose) {
        sendEventToUser(userId, Map.of(
                "type", "conversation_deleted",
                "partnerEmail", partnerEmail,
                "forceClose", forceClose), "conversation deletion");
    }

    private void sendEventToUser(Long userId, Object event, String eventDescription) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }

        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            logger.error("Failed to serialize {} for WebSocket delivery", eventDescription, exception);
            return;
        }

        TextMessage textMessage = new TextMessage(payload);
        for (WebSocketSession session : userSessions) {
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    } else {
                        removeSession(session);
                    }
                }
            } catch (IOException exception) {
                logger.warn("Failed to deliver {} over WebSocket", eventDescription, exception);
                removeSession(session);
            }
        }
    }

    private void removeSession(WebSocketSession session) {
        Object userId = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (userId instanceof Long id) {
            Set<WebSocketSession> userSessions = sessions.get(id);
            if (userSessions != null) {
                userSessions.remove(session);
                if (userSessions.isEmpty()) {
                    sessions.remove(id, userSessions);
                }
            }
        }
    }
}
