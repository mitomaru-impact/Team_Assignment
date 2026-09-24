package com.reme.re_me.dto;

public class AuthResponse {
    private String message;
    private Long userId;

    // デフォルトコンストラクタ（JSON変換用）
    public AuthResponse() {}

    public AuthResponse(String message, Long userId) {
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}