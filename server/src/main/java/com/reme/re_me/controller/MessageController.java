package com.reme.re_me.controller;

import com.reme.re_me.dto.ConversationDto;
import com.reme.re_me.dto.MessageDto;
import com.reme.re_me.dto.SendMessageRequest;
import com.reme.re_me.service.MessageService;
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

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
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
    public ResponseEntity<?> getMessages(@RequestParam Long userId, @RequestParam String targetEmail) {
        try {
            List<MessageDto> history = messageService.getChatHistory(userId, targetEmail);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 会話一覧の取得
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@RequestParam Long userId) {
        try {
            List<ConversationDto> conversations = messageService.getConversations(userId);
            return ResponseEntity.ok(conversations);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/read")
    public ResponseEntity<?> markConversationAsRead(
            @RequestParam Long userId,
            @RequestParam String targetEmail) {
        try {
            messageService.markConversationAsRead(userId, targetEmail);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}