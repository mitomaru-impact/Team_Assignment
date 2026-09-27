package com.reme.re_me.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "conversation_states",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversation_state_user_partner",
                columnNames = {"user_id", "partner_id"}))
public class ConversationState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "cleared_at", nullable = false)
    private LocalDateTime clearedAt;

    public ConversationState() {}

    public ConversationState(Long userId, Long partnerId, LocalDateTime clearedAt) {
        this.userId = userId;
        this.partnerId = partnerId;
        this.clearedAt = clearedAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getPartnerId() { return partnerId; }
    public LocalDateTime getClearedAt() { return clearedAt; }

    public void setClearedAt(LocalDateTime clearedAt) {
        this.clearedAt = clearedAt;
    }
}
