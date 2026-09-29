package com.reme.re_me.dto;

public class GroupContactDto {
    private final String publicUserId;
    private final String email;
    private final String displayName;

    public GroupContactDto(String publicUserId, String email, String displayName) {
        this.publicUserId = publicUserId;
        this.email = email;
        this.displayName = displayName;
    }

    public String getPublicUserId() { return publicUserId; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
}
