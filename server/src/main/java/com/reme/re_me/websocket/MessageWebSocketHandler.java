package com.reme.re_me.websocket;

import com.reme.re_me.dto.MessageDto;
import com.reme.re_me.dto.CallSignalRequest;
import com.reme.re_me.service.CallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;
import com.reme.re_me.service.PublicUserIdService;
import com.reme.re_me.service.SessionAuthService;
import com.reme.re_me.entity.User;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageWebSocketHandler.class);
    private static final String USER_ID_ATTRIBUTE = "userId";

    private final ObjectMapper objectMapper;
    private final PublicUserIdService publicUserIdService;
    private final SessionAuthService sessionAuthService;
    private final CallService callService;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public MessageWebSocketHandler(
            ObjectMapper objectMapper,
            PublicUserIdService publicUserIdService,
            SessionAuthService sessionAuthService,
            CallService callService
    ) {
        this.objectMapper = objectMapper;
        this.publicUserIdService = publicUserIdService;
        this.sessionAuthService = sessionAuthService;
        this.callService = callService;
    }

    public void sendCallEvent(Long userId, Object event) {
        sendEventToUser(userId, event, "call signaling");
    }

    @EventListener
    public void deliverUserEvent(UserWebSocketEvent event) {
        sendCallEvent(event.userId(), event.payload());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        session.setTextMessageSizeLimit(300 * 1024);
        String userIdParameter = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("userId");
        try {
            if (userIdParameter == null) {
                throw new NumberFormatException("Missing userId");
            }
            User authenticatedUser = sessionAuthService.requireUser(
                    session.getHandshakeHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            Long userId = publicUserIdService.resolveInternalId(userIdParameter);
            if (!authenticatedUser.getId().equals(userId)) {
                throw new IllegalArgumentException("Session user does not match userId");
            }
            session.getAttributes().put(USER_ID_ATTRIBUTE, userId);
            sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        } catch (IllegalArgumentException | org.springframework.web.server.ResponseStatusException exception) {
            logger.warn("Rejected WebSocket connection with invalid user identity");
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Object authenticatedUserId = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (!(authenticatedUserId instanceof Long userId)) {
            closeInvalidSession(session);
            return;
        }

        String callId = null;
        try {
            CallSignalRequest signal = objectMapper.readValue(message.getPayload(), CallSignalRequest.class);
            callId = signal.callId();
            callService.relaySignal(userId, signal);
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("type", "call_signal_error");
            error.put("message", exception.getReason() == null
                    ? "通話シグナリングを受け付けられません" : exception.getReason());
            if (callId != null) error.put("callId", callId);
            sendEventToUser(userId, error, "call signaling error");
        } catch (JacksonException exception) {
            logger.warn("Rejected malformed WebSocket signaling payload");
            sendEventToUser(userId, Map.of(
                    "type", "call_signal_error",
                    "message", "通話シグナリングの形式が正しくありません"
            ), "call signaling error");
        }
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

    public void sendReadReceipt(Long userId, String readerId, List<Long> messageIds) {
        sendEventToUser(userId, Map.of(
                "type", "read_receipt",
                "readerId", readerId,
                "messageIds", messageIds), "read receipt");
    }

    public void sendConversationDeleted(Long userId, String partnerEmail, boolean forceClose) {
        sendEventToUser(userId, Map.of(
                "type", "conversation_deleted",
                "partnerEmail", partnerEmail,
                "forceClose", forceClose), "conversation deletion");
    }

    public void sendProfileIdentityChanged(Long userId, String profileOwnerEmail) {
        sendEventToUser(userId, Map.of(
                "type", "profile_identity_changed",
                "partnerEmail", profileOwnerEmail), "profile identity change");
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

    private void closeInvalidSession(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.BAD_DATA);
            }
        } catch (IOException exception) {
            logger.warn("Failed to close an invalid WebSocket session", exception);
        }
    }
}
