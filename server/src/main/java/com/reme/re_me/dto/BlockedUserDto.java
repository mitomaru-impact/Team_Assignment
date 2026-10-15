package com.reme.re_me.dto;

public record BlockedUserDto(
        String email,
        String publicUserId,
        Long visibleProfileId,
        String visibleProfileName) {}
