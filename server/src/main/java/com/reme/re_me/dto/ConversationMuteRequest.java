package com.reme.re_me.dto;

public record ConversationMuteRequest(
        String userId,
        String targetEmail,
        String groupId,
        String mode
) {}
