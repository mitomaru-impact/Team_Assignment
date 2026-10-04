package com.reme.re_me.dto;

public record MessageMentionDto(
        int startIndex,
        int length,
        String userId,
        String displayName,
        boolean currentUser,
        boolean everyone
) {}
