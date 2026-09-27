package com.reme.re_me.dto;

public class AuthResponse {
    private String message;
    private String userId;

    // デフォルトコンストラクタ（JSON変換用）
    public AuthResponse() {}

    public AuthResponse(String message, String userId) {
        this.message = message;
        this.userId = userId;
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
}