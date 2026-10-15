package com.reme.re_me.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "user_blocks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_block",
                columnNames = {"blocker_id", "blocked_id"}))
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;

    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;

    @Column(name = "visible_profile_id")
    private Long visibleProfileId;

    protected UserBlock() {}

    public UserBlock(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }

    public Long getBlockerId() {
        return blockerId;
    }

    public Long getBlockedId() {
        return blockedId;
    }

    public Long getVisibleProfileId() {
        return visibleProfileId;
    }

    public void setVisibleProfileId(Long visibleProfileId) {
        this.visibleProfileId = visibleProfileId;
    }
}
