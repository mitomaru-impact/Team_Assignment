package com.reme.re_me.dto;

public class GroupMemberDto {
    private final String publicUserId;
    private final String email;
    private final String displayName;
    private final String username;
    private final boolean creator;

    public GroupMemberDto(
            String publicUserId, String email, String displayName, String username, boolean creator) {
        this.publicUserId = publicUserId;
        this.email = email;
        this.displayName = displayName;
        this.username = username;
        this.creator = creator;
    }

    public String getPublicUserId() { return publicUserId; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getUsername() { return username; }
    public boolean isCreator() { return creator; }
}
