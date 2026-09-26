package com.reme.re_me.dto;

import com.reme.re_me.entity.Message;
import java.time.LocalDateTime;

public class MessageDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime createdAt;

    public MessageDto(Message message) {
        this.id = message.getId();
        this.senderId = message.getSenderId();
        this.receiverId = message.getReceiverId();
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
    }

    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public Long getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}