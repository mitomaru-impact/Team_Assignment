package com.reme.re_me.service;

import com.reme.re_me.entity.ApnsDeviceToken;
import com.reme.re_me.repository.ApnsDeviceTokenRepository;
import com.reme.re_me.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ApnsDeviceTokenService {

    private static final Pattern HEX_TOKEN = Pattern.compile("[0-9a-fA-F]+");

    private final ApnsDeviceTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public ApnsDeviceTokenService(ApnsDeviceTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void register(Long userId, String deviceToken) {
        if (userId == null || deviceToken == null || deviceToken.length() < 64
                || deviceToken.length() > 256 || deviceToken.length() % 2 != 0
                || !HEX_TOKEN.matcher(deviceToken).matches()) {
            throw new IllegalArgumentException("有効なユーザーIDとAPNsデバイストークンが必要です");
        }
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("ユーザーが見つかりません");
        }

        String normalizedToken = deviceToken.toLowerCase(Locale.ROOT);
        ApnsDeviceToken token = tokenRepository.findByToken(normalizedToken)
                .orElseGet(() -> new ApnsDeviceToken(normalizedToken, userId));
        token.setUserId(userId);
        tokenRepository.save(token);
    }

    @Transactional
    public void unregister(Long userId, String deviceToken) {
        if (userId == null || deviceToken == null) {
            throw new IllegalArgumentException("ユーザーIDとAPNsデバイストークンが必要です");
        }
        tokenRepository.deleteByTokenAndUserId(deviceToken.toLowerCase(Locale.ROOT), userId);
    }
}
