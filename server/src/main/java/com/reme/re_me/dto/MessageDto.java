package com.reme.re_me.dto;

import com.reme.re_me.entity.Message;
import java.time.LocalDateTime;

public class MessageDto {
    private Long id;
    private String senderId;
    private String receiverId;
    private String content;
    private String senderDisplayName;
    private Long replyToMessageId;
    private String replyToContent;
    private String replyToSenderName;
    private String type;
    private Long callDurationSeconds;
    private LocalDateTime createdAt;

    public MessageDto(Message message, String senderId, String receiverId) {
        this(message, senderId, receiverId, message.getSenderDisplayName());
    }

    public MessageDto(Message message, String senderId, String receiverId, String senderDisplayName) {
        this.id = message.getId();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = message.getContent();
        this.senderDisplayName = senderDisplayName;
        this.replyToMessageId = message.getReplyToMessageId();
        this.replyToContent = message.getReplyToContent();
        this.replyToSenderName = message.getReplyToSenderName();
        this.type = message.getMessageType();
        this.callDurationSeconds = message.getCallDurationSeconds();
        this.createdAt = message.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public String getSenderDisplayName() { return senderDisplayName; }
    public Long getReplyToMessageId() { return replyToMessageId; }
    public String getReplyToContent() { return replyToContent; }
    public String getReplyToSenderName() { return replyToSenderName; }
    public String getType() { return type; }
    public Long getCallDurationSeconds() { return callDurationSeconds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}