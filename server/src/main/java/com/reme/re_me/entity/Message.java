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

    @Column(name = "sender_display_name", length = 255)
    private String senderDisplayName;

    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Column(name = "reply_to_content", length = 1000)
    private String replyToContent;

    @Column(name = "reply_to_sender_name", length = 255)
    private String replyToSenderName;

    @Column(name = "message_type", length = 24)
    private String messageType = "MESSAGE";

    @Column(name = "call_id", unique = true, length = 36)
    private String callId;

    @Column(name = "call_duration_seconds")
    private Long callDurationSeconds;

    @Column(name = "read_status")
    private Boolean readStatus = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean edited = false;

    public Message() {}

    public Message(Long senderId, Long receiverId, String content) {
        this(senderId, receiverId, content, null, null, null, null);
    }

    public Message(
            Long senderId,
            Long receiverId,
            String content,
            String senderDisplayName,
            Long replyToMessageId,
            String replyToContent,
            String replyToSenderName) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.senderDisplayName = senderDisplayName;
        this.replyToMessageId = replyToMessageId;
        this.replyToContent = replyToContent;
        this.replyToSenderName = replyToSenderName;
        this.createdAt = LocalDateTime.now();
    }

    public static Message callLog(Long callerId, Long calleeId, String callId, long durationSeconds) {
        Message message = new Message(callerId, calleeId, "通話終了");
        message.messageType = "CALL_LOG";
        message.callId = callId;
        message.callDurationSeconds = Math.max(0, durationSeconds);
        return message;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public Long getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public Boolean getEdited() { return edited; }
    public String getSenderDisplayName() { return senderDisplayName; }
    public Long getReplyToMessageId() { return replyToMessageId; }
    public String getReplyToContent() { return replyToContent; }
    public String getReplyToSenderName() { return replyToSenderName; }
    public String getMessageType() { return messageType; }
    public String getCallId() { return callId; }
    public Long getCallDurationSeconds() { return callDurationSeconds; }
    public Boolean getReadStatus() { return readStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setReadStatus(Boolean readStatus) { this.readStatus = readStatus; }
    public void setContent(String content) {
        this.content = content;
        this.edited = true;
    }
}