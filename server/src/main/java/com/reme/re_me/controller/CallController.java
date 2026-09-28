package com.reme.re_me.controller;

import com.reme.re_me.dto.CallInvitationResponse;
import com.reme.re_me.dto.StartCallRequest;
import com.reme.re_me.entity.User;
import com.reme.re_me.service.CallService;
import com.reme.re_me.service.SessionAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CallService callService;
    private final SessionAuthService sessionAuthService;

    public CallController(CallService callService, SessionAuthService sessionAuthService) {
        this.callService = callService;
        this.sessionAuthService = sessionAuthService;
    }

    @PostMapping
    public CallInvitationResponse start(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody StartCallRequest request
    ) {
        User caller = sessionAuthService.requireUser(authorization);
        return callService.startCall(caller, request.targetEmail());
    }

    @PostMapping("/{callId}/accept")
    public CallInvitationResponse accept(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String callId
    ) {
        return callService.accept(sessionAuthService.requireUser(authorization), callId);
    }

    @PostMapping("/{callId}/decline")
    public ResponseEntity<Void> decline(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String callId
    ) {
        callService.decline(sessionAuthService.requireUser(authorization), callId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{callId}/end")
    public ResponseEntity<Void> end(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String callId
    ) {
        callService.end(sessionAuthService.requireUser(authorization), callId);
        return ResponseEntity.noContent().build();
    }

}
