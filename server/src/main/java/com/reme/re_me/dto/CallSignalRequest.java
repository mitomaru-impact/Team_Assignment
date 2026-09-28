package com.reme.re_me.dto;

public record CallSignalRequest(
        String type,
        String callId,
        String sdp,
        String candidate,
        String sdpMid,
        Integer sdpMLineIndex
) {}
