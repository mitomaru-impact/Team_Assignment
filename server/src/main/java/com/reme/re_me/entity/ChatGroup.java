package com.reme.re_me.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_groups")
public class ChatGroup {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ChatGroup() {}

    public ChatGroup(String name, Long createdBy) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
