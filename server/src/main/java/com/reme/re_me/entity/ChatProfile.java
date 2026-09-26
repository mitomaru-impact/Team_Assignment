package com.reme.re_me.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_profiles")
public class ChatProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false)
    private boolean defaultProfile;

    public ChatProfile() {}

    public ChatProfile(Long userId, String name, boolean defaultProfile) {
        this.userId = userId;
        this.name = name;
        this.defaultProfile = defaultProfile;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public boolean isDefaultProfile() { return defaultProfile; }
}
