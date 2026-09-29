package com.reme.re_me.dto;

import java.util.List;

public class CreateGroupRequest {
    private String userId;
    private Long profileId;
    private String name;
    private List<String> memberPublicIds;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getMemberPublicIds() { return memberPublicIds; }
    public void setMemberPublicIds(List<String> memberPublicIds) { this.memberPublicIds = memberPublicIds; }
}
