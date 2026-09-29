package com.reme.re_me.dto;

public class GroupMemberDto {
    private final String publicUserId;
    private final String email;
    private final String displayName;
    private final boolean creator;

    public GroupMemberDto(String publicUserId, String email, String displayName, boolean creator) {
        this.publicUserId = publicUserId;
        this.email = email;
        this.displayName = displayName;
        this.creator = creator;
    }

    public String getPublicUserId() { return publicUserId; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public boolean isCreator() { return creator; }
}
