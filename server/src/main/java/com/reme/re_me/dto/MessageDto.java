package com.reme.re_me.dto;

import com.reme.re_me.entity.Message;
import java.time.LocalDateTime;

public class MessageDto {
    private Long id;
    private String senderId;
    private String receiverId;
    private String content;
    private Long replyToMessageId;
    private String replyToContent;
    private String replyToSenderName;
    private LocalDateTime createdAt;

    public MessageDto(Message message, String senderId, String receiverId) {
        this.id = message.getId();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = message.getContent();
        this.replyToMessageId = message.getReplyToMessageId();
        this.replyToContent = message.getReplyToContent();
        this.replyToSenderName = message.getReplyToSenderName();
        this.createdAt = message.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public Long getReplyToMessageId() { return replyToMessageId; }
    public String getReplyToContent() { return replyToContent; }
    public String getReplyToSenderName() { return replyToSenderName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}