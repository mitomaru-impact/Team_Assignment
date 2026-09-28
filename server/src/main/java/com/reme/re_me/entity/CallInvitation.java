package com.reme.re_me.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "call_invitations")
public class CallInvitation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "room_name", nullable = false, unique = true, length = 80)
    private String roomName;

    @Column(name = "caller_user_id", nullable = false)
    private Long callerUserId;

    @Column(name = "callee_user_id", nullable = false)
    private Long calleeUserId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected CallInvitation() {}

    public CallInvitation(String id, String roomName, Long callerUserId, Long calleeUserId,
                          String status, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.roomName = roomName;
        this.callerUserId = callerUserId;
        this.calleeUserId = calleeUserId;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public String getRoomName() { return roomName; }
    public Long getCallerUserId() { return callerUserId; }
    public Long getCalleeUserId() { return calleeUserId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
}
