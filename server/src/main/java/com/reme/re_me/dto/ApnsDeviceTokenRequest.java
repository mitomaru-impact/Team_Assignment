package com.reme.re_me.dto;

public class ApnsDeviceTokenRequest {
    private String userId;
    private String deviceToken;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
}
