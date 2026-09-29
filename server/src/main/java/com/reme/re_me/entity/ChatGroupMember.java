package com.reme.re_me.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_group_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_group_member",
                columnNames = {"group_id", "user_id"}))
public class ChatGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false, length = 36)
    private String groupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    public ChatGroupMember() {}

    public ChatGroupMember(String groupId, Long userId) {
        this.groupId = groupId;
        this.userId = userId;
        this.lastReadAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getGroupId() { return groupId; }
    public Long getUserId() { return userId; }
    public LocalDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }
}
