package com.reme.re_me.dto;

import com.reme.re_me.entity.GroupMessage;
import java.time.LocalDateTime;

public class GroupConversationDto {
    private final String id;
    private final String name;
    private final String lastMessage;
    private final LocalDateTime lastMessageTime;
    private final long unreadCount;

    public GroupConversationDto(String id, String name, GroupMessage lastMessage, long unreadCount) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage == null ? "" : lastMessage.getContent();
        this.lastMessageTime = lastMessage == null ? null : lastMessage.getCreatedAt();
        this.unreadCount = unreadCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public LocalDateTime getLastMessageTime() { return lastMessageTime; }
    public long getUnreadCount() { return unreadCount; }
}
