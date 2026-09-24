package com.reme.re_me.service;

import com.reme.re_me.dto.LoginRequest;
import com.reme.re_me.dto.RegisterRequest;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ユーザー登録処理
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("このメールアドレスは既に登録されています");
        }

        User user = new User(
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()), // パスワードを暗号化
            request.getName()
        );

        return userRepository.save(user);
    }

    // ログイン検証処理
    public User login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("メールアドレスまたはパスワードが正しくありません"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("メールアドレスまたはパスワードが正しくありません");
        }

        return user;
    }
}