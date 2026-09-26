package com.reme.re_me.dto;

public class SendMessageRequest {
    private Long senderId;
    private String receiverEmail; // 相手のメールアドレス指定
    private String content;

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}