package com.reme.re_me.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private Long receiverId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "read_status")
    private Boolean readStatus = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Message() {}

    public Message(Long senderId, Long receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public Long getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public Boolean getReadStatus() { return readStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setReadStatus(Boolean readStatus) { this.readStatus = readStatus; }
}