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

    @Column(name = "display_name", length = 40)
    private String displayName;

    @Column(nullable = false)
    private boolean defaultProfile;

    public ChatProfile() {}

    public ChatProfile(Long userId, String name, boolean defaultProfile) {
        this(userId, name, name, defaultProfile);
    }

    public ChatProfile(Long userId, String name, String displayName, boolean defaultProfile) {
        this.userId = userId;
        this.name = name;
        this.displayName = displayName;
        this.defaultProfile = defaultProfile;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isDefaultProfile() { return defaultProfile; }
}
