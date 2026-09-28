package com.reme.re_me.controller;

import com.reme.re_me.dto.ConversationDto;
import com.reme.re_me.dto.MessageDto;
import com.reme.re_me.dto.SendMessageRequest;
import com.reme.re_me.dto.EditMessageRequest;
import com.reme.re_me.service.ChatProfileService;
import com.reme.re_me.service.MessageService;
import com.reme.re_me.service.PublicUserIdService;
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

    public MessageController(
            MessageService messageService,
            ChatProfileService chatProfileService,
            PublicUserIdService publicUserIdService) {
        this.messageService = messageService;
        this.chatProfileService = chatProfileService;
        this.publicUserIdService = publicUserIdService;
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
}