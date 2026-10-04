package com.reme.re_me.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "conversation_mutes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversation_mute_partner",
                        columnNames = {"user_id", "partner_id"}),
                @UniqueConstraint(
                        name = "uk_conversation_mute_group",
                        columnNames = {"user_id", "group_id"})
        })
public class ConversationMute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "group_id", length = 36)
    private String groupId;

    @Column(nullable = false, length = 24)
    private String mode;

    @Column(nullable = false)
    private boolean muted;

    @Column(name = "notify_mentions", nullable = false)
    private boolean notifyMentions;

    public ConversationMute() {}

    public ConversationMute(Long userId, Long partnerId, String groupId, String mode) {
        this.userId = userId;
        this.partnerId = partnerId;
        this.groupId = groupId;
        this.mode = mode;
        this.muted = !"NONE".equals(mode);
        this.notifyMentions = "MENTIONS_ONLY".equals(mode);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getPartnerUserId() { return partnerId; }
    public String getGroupId() { return groupId; }
    public String getMode() { return mode; }

    public void setMode(String mode) {
        this.mode = mode;
        this.muted = !"NONE".equals(mode);
        this.notifyMentions = "MENTIONS_ONLY".equals(mode);
    }
}
