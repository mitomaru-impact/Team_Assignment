package com.reme.re_me.dto;

import java.time.LocalDateTime;

public class ConversationDto {
    private String targetEmail;
    private String targetName;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;

    public ConversationDto(String targetEmail, String targetName, String lastMessage,
                           LocalDateTime lastMessageTime, long unreadCount) {
        this.targetEmail = targetEmail;
        this.targetName = targetName;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
    }

    public String getTargetEmail() { return targetEmail; }
    public String getTargetName() { return targetName; }
    public String getLastMessage() { return lastMessage; }
    public LocalDateTime getLastMessageTime() { return lastMessageTime; }
    public long getUnreadCount() { return unreadCount; }
}