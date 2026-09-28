package com.reme.re_me.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "voip_device_tokens")
public class VoipDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 256)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected VoipDeviceToken() {}

    public VoipDeviceToken(String token, Long userId) {
        this.token = token;
        this.userId = userId;
        this.updatedAt = LocalDateTime.now();
    }

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    public void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
