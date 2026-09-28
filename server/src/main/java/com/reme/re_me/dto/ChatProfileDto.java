package com.reme.re_me.dto;

import com.reme.re_me.entity.ChatProfile;

public class ChatProfileDto {
    private final Long id;
    private final String name;
    private final String displayName;
    private final boolean defaultProfile;

    public ChatProfileDto(ChatProfile profile) {
        this.id = profile.getId();
        this.name = profile.getName();
        this.displayName = profile.getDisplayName();
        this.defaultProfile = profile.isDefaultProfile();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public boolean isDefaultProfile() { return defaultProfile; }
}
