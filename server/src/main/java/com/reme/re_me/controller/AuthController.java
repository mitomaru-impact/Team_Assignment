package com.reme.re_me.controller;

import com.reme.re_me.dto.AuthResponse;
import com.reme.re_me.dto.LoginRequest;
import com.reme.re_me.dto.RegisterRequest;
import com.reme.re_me.entity.User;
import com.reme.re_me.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        System.out.println("★ 登録リクエストを受信: " + request.getEmail());
        try {
            User registeredUser = authService.register(request);
            return ResponseEntity.ok(new AuthResponse("ユーザー登録が完了しました", registeredUser.getId()));
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
            return ResponseEntity.ok(new AuthResponse("ログインに成功しました", user.getId()));
        } catch (Exception e) {
            System.out.println("★ ログインエラー: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}