package com.reme.re_me.dto;

public class ApnsDeviceTokenRequest {
    private Long userId;
    private String deviceToken;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
}
