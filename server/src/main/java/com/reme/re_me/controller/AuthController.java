package com.reme.re_me.controller;

import com.reme.re_me.dto.AuthResponse;
import com.reme.re_me.dto.LoginRequest;
import com.reme.re_me.dto.RegisterRequest;
import com.reme.re_me.entity.User;
import com.reme.re_me.service.AuthService;
import com.reme.re_me.service.SessionAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionAuthService sessionAuthService;

    public AuthController(AuthService authService, SessionAuthService sessionAuthService) {
        this.authService = authService;
        this.sessionAuthService = sessionAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        System.out.println("★ 登録リクエストを受信: " + request.getEmail());
        try {
            User registeredUser = authService.register(request);
            String sessionToken = sessionAuthService.issueToken(registeredUser);
            return ResponseEntity.ok(new AuthResponse("ユーザー登録が完了しました", registeredUser.getPublicId(), sessionToken));
        } catch (Exception e) {
            System.out.println("★ 登録エラー: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        System.out.println("★ ログインリクエストを受信: " + request.getEmail());
        try {
            User user = authService.login(request);
            String sessionToken = sessionAuthService.issueToken(user);
            return ResponseEntity.ok(new AuthResponse("ログインに成功しました", user.getPublicId(), sessionToken));
        } catch (Exception e) {
            System.out.println("★ ログインエラー: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        sessionAuthService.revoke(authorization);
        return ResponseEntity.noContent().build();
    }
}