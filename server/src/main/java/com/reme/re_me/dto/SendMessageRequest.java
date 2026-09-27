package com.reme.re_me.dto;

public class SendMessageRequest {
    private String senderId;
    private Long profileId;
    private Long replyToMessageId;
    private String receiverEmail; // 相手のメールアドレス指定
    private String content;

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
    public Long getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Long replyToMessageId) { this.replyToMessageId = replyToMessageId; }
    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}