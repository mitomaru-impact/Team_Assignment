package com.reme.re_me.dto;

public class AuthResponse {
    private String message;
    private String userId;
    private String sessionToken;

    // デフォルトコンストラクタ（JSON変換用）
    public AuthResponse() {}

    public AuthResponse(String message, String userId) {
        this(message, userId, null);
    }

    public AuthResponse(String message, String userId, String sessionToken) {
        this.message = message;
        this.userId = userId;
        this.sessionToken = sessionToken;
    }

    // ★重要: GetterがないとJSONに変換されません
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}