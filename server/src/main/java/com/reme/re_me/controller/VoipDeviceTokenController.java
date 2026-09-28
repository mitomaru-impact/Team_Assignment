package com.reme.re_me.controller;

import com.reme.re_me.dto.VoipDeviceTokenRequest;
import com.reme.re_me.entity.User;
import com.reme.re_me.service.SessionAuthService;
import com.reme.re_me.service.VoipDeviceTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices/voip")
public class VoipDeviceTokenController {

    private final VoipDeviceTokenService tokenService;
    private final SessionAuthService sessionAuthService;

    public VoipDeviceTokenController(
            VoipDeviceTokenService tokenService,
            SessionAuthService sessionAuthService
    ) {
        this.tokenService = tokenService;
        this.sessionAuthService = sessionAuthService;
    }

    @PostMapping
    public ResponseEntity<Void> register(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody VoipDeviceTokenRequest request
    ) {
        User user = sessionAuthService.requireUser(authorization);
        tokenService.register(user.getId(), request.deviceToken());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unregister(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceToken
    ) {
        User user = sessionAuthService.requireUser(authorization);
        tokenService.unregister(user.getId(), deviceToken);
        return ResponseEntity.noContent().build();
    }
}
