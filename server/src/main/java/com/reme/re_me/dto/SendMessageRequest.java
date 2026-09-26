package com.reme.re_me.dto;

public class SendMessageRequest {
    private Long senderId;
    private Long profileId;
    private String receiverEmail; // 相手のメールアドレス指定
    private String content;

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }

    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}