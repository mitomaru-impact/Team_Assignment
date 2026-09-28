package com.reme.re_me.service;

import com.reme.re_me.entity.User;
import com.reme.re_me.entity.UserSession;
import com.reme.re_me.repository.UserSessionRepository;
import com.reme.re_me.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class SessionAuthService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    public SessionAuthService(UserSessionRepository userSessionRepository, UserRepository userRepository) {
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String issueToken(User user) {
        Instant now = Instant.now();
        userSessionRepository.deleteByExpiresAtBefore(now);
        String token = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        userSessionRepository.save(new UserSession(hash(token), user.getId(), now.plus(30, ChronoUnit.DAYS)));
        return token;
    }

    @Transactional(readOnly = true)
    public User requireUser(String authorizationHeader) {
        String token = bearerToken(authorizationHeader);
        UserSession session = userSessionRepository
                .findByTokenHashAndExpiresAtAfter(hash(token), Instant.now())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "通話セッションの有効期限が切れました。再ログインしてください"));
        return userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "アカウントが見つかりません"));
    }

    @Transactional
    public void revoke(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (!token.isEmpty()) {
            userSessionRepository.deleteById(hash(token));
        }
    }

    private String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "通話の認証が必要です");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "通話の認証が必要です");
        }
        return token;
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
