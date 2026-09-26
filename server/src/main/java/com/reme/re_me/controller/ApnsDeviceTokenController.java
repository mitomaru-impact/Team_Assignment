package com.reme.re_me.controller;

import com.reme.re_me.dto.ApnsDeviceTokenRequest;
import com.reme.re_me.service.ApnsDeviceTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices/apns")
public class ApnsDeviceTokenController {

    private static final Logger logger = LoggerFactory.getLogger(ApnsDeviceTokenController.class);
    private final ApnsDeviceTokenService tokenService;

    public ApnsDeviceTokenController(ApnsDeviceTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<?> register(@RequestBody ApnsDeviceTokenRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("APNsデバイストークンの登録情報が必要です");
        }
        try {
            tokenService.register(request.getUserId(), request.getDeviceToken());
            logger.info("Registered APNs device token for user {}", request.getUserId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Rejected APNs device token registration: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> unregister(@RequestParam Long userId, @RequestParam String deviceToken) {
        try {
            tokenService.unregister(userId, deviceToken);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
