package com.reme.re_me.dto;

public record TypingSignalRequest(
        String type,
        String targetEmail,
        String groupId,
        Boolean isTyping
) {}
