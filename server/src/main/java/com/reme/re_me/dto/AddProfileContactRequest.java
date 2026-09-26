package com.reme.re_me.dto;

public class AddProfileContactRequest {
    private Long userId;
    private String email;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
