package com.reme.re_me.dto;

import java.util.List;

public class ManageGroupMembersRequest {
    private String userId;
    private List<String> memberPublicIds;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<String> getMemberPublicIds() { return memberPublicIds; }
    public void setMemberPublicIds(List<String> memberPublicIds) { this.memberPublicIds = memberPublicIds; }
}
