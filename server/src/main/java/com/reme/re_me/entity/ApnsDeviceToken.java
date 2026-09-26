package com.reme.re_me.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "apns_device_tokens")
public class ApnsDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 256)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ApnsDeviceToken() {}

    public ApnsDeviceToken(String token, Long userId) {
        this.token = token;
        this.userId = userId;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    @PrePersist
    public void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
