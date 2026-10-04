package com.reme.re_me.controller;

import com.reme.re_me.dto.ConversationDto;
import com.reme.re_me.dto.MessageDto;
import com.reme.re_me.dto.SendMessageRequest;
import com.reme.re_me.dto.EditMessageRequest;
import com.reme.re_me.dto.ToggleMessageReactionRequest;
import com.reme.re_me.dto.ConversationMuteRequest;
import com.reme.re_me.service.ConversationMuteService;
import com.reme.re_me.service.ChatProfileService;
import com.reme.re_me.service.MessageReactionService;
import com.reme.re_me.service.MessageRecallService;
import com.reme.re_me.service.MessageService;
import com.reme.re_me.service.PublicUserIdService;
import com.reme.re_me.service.UnreadBadgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
    private final MessageService messageService;
    private final ChatProfileService chatProfileService;
    private final PublicUserIdService publicUserIdService;
    private final MessageReactionService messageReactionService;
    private final MessageRecallService messageRecallService;
    private final UnreadBadgeService unreadBadgeService;
    private final ConversationMuteService conversationMuteService;

    public MessageController(
            MessageService messageService,
            ChatProfileService chatProfileService,
            PublicUserIdService publicUserIdService,
            MessageReactionService messageReactionService,
            MessageRecallService messageRecallService,
            UnreadBadgeService unreadBadgeService,
            ConversationMuteService conversationMuteService) {
        this.messageService = messageService;
        this.chatProfileService = chatProfileService;
        this.publicUserIdService = publicUserIdService;
        this.messageReactionService = messageReactionService;
        this.messageRecallService = messageRecallService;
        this.unreadBadgeService = unreadBadgeService;
        this.conversationMuteService = conversationMuteService;
    }

    @GetMapping("/mutes")
    public ResponseEntity<?> getConversationMutes(@RequestParam String userId) {
        try {
            return ResponseEntity.ok(conversationMuteService.list(userId));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PutMapping("/mute")
    public ResponseEntity<?> updateConversationMute(@RequestBody ConversationMuteRequest request) {
        try {
            conversationMuteService.update(request);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    // メッセージ送信
    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("メッセージの送信内容が必要です");
        }
        logger.info("Received message send request from user {}", request.getSenderId());
        try {
            MessageDto message = messageService.sendMessage(request);
            logger.info("Message {} saved for recipient {}", message.getId(), message.getReceiverId());
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            logger.error("Failed to send message for user {}: {}",
                    request.getSenderId(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // メッセージ履歴取得
    @GetMapping
    public ResponseEntity<?> getMessages(@RequestParam String userId, @RequestParam String targetEmail) {
        try {
            List<MessageDto> history = messageService.getChatHistory(
                    publicUserIdService.resolveInternalId(userId), targetEmail);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestParam String userId) {
        try {
            return ResponseEntity.ok(unreadBadgeService.unreadCount(
                    publicUserIdService.resolveInternalId(userId)));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<?> editMessage(
            @PathVariable Long messageId,
            @RequestBody EditMessageRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("編集内容が必要です");
        }

        try {
            return ResponseEntity.ok(messageService.editMessage(
                    publicUserIdService.resolveInternalId(request.getUserId()),
                    messageId,
                    request.getContent()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<?> toggleReaction(
            @PathVariable Long messageId,
            @RequestBody ToggleMessageReactionRequest request) {
        if (request == null || request.userId() == null) {
            return ResponseEntity.badRequest().body("リアクションするユーザーが必要です");
        }
        try {
            messageReactionService.toggleDirect(
                    publicUserIdService.resolveInternalId(request.userId()),
                    messageId,
                    request.emoji());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> recallMessage(
            @PathVariable Long messageId,
            @RequestParam String userId) {
        try {
            messageRecallService.recallDirect(
                    publicUserIdService.resolveInternalId(userId),
                    messageId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    // 会話一覧の取得
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(
            @RequestParam String userId,
            @RequestParam(required = false) Long profileId,
            @RequestParam(defaultValue = "false") boolean unclassified) {
        try {
            List<ConversationDto> conversations;
            Long internalUserId = publicUserIdService.resolveInternalId(userId);
            if (unclassified) {
                conversations = messageService.getUnclassifiedConversations(internalUserId);
            } else {
                if (profileId == null) {
                    return ResponseEntity.badRequest().body("プロファイルIDが必要です");
                }
                conversations = messageService.getConversations(internalUserId, profileId, false);
            }
            return ResponseEntity.ok(conversations);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/read")
    public ResponseEntity<?> markConversationAsRead(
            @RequestParam String userId,
            @RequestParam String targetEmail) {
        try {
            messageService.markConversationAsRead(publicUserIdService.resolveInternalId(userId), targetEmail);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/conversation")
    public ResponseEntity<?> deleteConversation(
            @RequestParam String userId,
            @RequestParam String targetUserId,
            @RequestParam(required = false) Long profileId,
            @RequestParam(defaultValue = "false") boolean deleteForBoth) {
        try {
            chatProfileService.deleteConversation(
                    publicUserIdService.resolveInternalId(userId),
                    publicUserIdService.resolveInternalId(targetUserId),
                    profileId,
                    deleteForBoth);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/conversation/by-email")
    public ResponseEntity<?> deleteConversationByEmail(
            @RequestParam String userId,
            @RequestParam String targetEmail,
            @RequestParam(required = false) Long profileId,
            @RequestParam(defaultValue = "false") boolean deleteForBoth) {
        try {
            messageService.deleteConversation(
                    publicUserIdService.resolveInternalId(userId),
                    targetEmail,
                    profileId,
                    deleteForBoth);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/leave")
    public ResponseEntity<?> leaveConversation(
            @RequestParam String userId,
            @RequestParam String targetEmail,
            @RequestParam(required = false) Long profileId) {
        try {
            messageService.leaveConversation(
                    publicUserIdService.resolveInternalId(userId), targetEmail, profileId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}