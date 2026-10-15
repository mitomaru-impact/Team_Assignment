package com.reme.re_me.dto;

public record UpdateBlockedUserProfileRequest(
        String userId,
        String targetEmail,
        Long profileId) {}
