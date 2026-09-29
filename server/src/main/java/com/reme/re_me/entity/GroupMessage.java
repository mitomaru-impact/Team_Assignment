package com.reme.re_me.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages")
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false, length = 36)
    private String groupId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_display_name", nullable = false, length = 255)
    private String senderDisplayName;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Column(name = "reply_to_content", length = 1000)
    private String replyToContent;

    @Column(name = "reply_to_sender_name", length = 255)
    private String replyToSenderName;

    @Column(nullable = false)
    private Boolean edited = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GroupMessage() {}

    public GroupMessage(
            String groupId,
            Long senderId,
            String senderDisplayName,
            String content,
            GroupMessage replyTo) {
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderDisplayName = senderDisplayName;
        this.content = content;
        this.replyToMessageId = replyTo == null ? null : replyTo.getId();
        this.replyToContent = replyTo == null ? null : replyTo.getContent();
        this.replyToSenderName = replyTo == null ? null : replyTo.getSenderDisplayName();
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getGroupId() { return groupId; }
    public Long getSenderId() { return senderId; }
    public String getSenderDisplayName() { return senderDisplayName; }
    public String getContent() { return content; }
    public Long getReplyToMessageId() { return replyToMessageId; }
    public String getReplyToContent() { return replyToContent; }
    public String getReplyToSenderName() { return replyToSenderName; }
    public Boolean getEdited() { return edited; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setContent(String content) {
        this.content = content;
        this.edited = true;
    }
}
