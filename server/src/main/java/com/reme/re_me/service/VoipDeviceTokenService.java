package com.reme.re_me.service;

import com.reme.re_me.entity.VoipDeviceToken;
import com.reme.re_me.repository.UserRepository;
import com.reme.re_me.repository.VoipDeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class VoipDeviceTokenService {

    private static final Pattern HEX_TOKEN = Pattern.compile("[0-9a-fA-F]{64,256}");

    private final VoipDeviceTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public VoipDeviceTokenService(VoipDeviceTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void register(Long userId, String deviceToken) {
        if (userId == null || deviceToken == null || deviceToken.length() % 2 != 0
                || !HEX_TOKEN.matcher(deviceToken).matches()) {
            throw new IllegalArgumentException("有効なユーザーIDとVoIPデバイストークンが必要です");
        }
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("ユーザーが見つかりません");
        }

        String normalized = deviceToken.toLowerCase(Locale.ROOT);
        VoipDeviceToken token = tokenRepository.findByToken(normalized)
                .orElseGet(() -> new VoipDeviceToken(normalized, userId));
        token.setUserId(userId);
        tokenRepository.save(token);
    }

    @Transactional
    public void unregister(Long userId, String deviceToken) {
        if (userId == null || deviceToken == null) {
            throw new IllegalArgumentException("ユーザーIDとVoIPデバイストークンが必要です");
        }
        tokenRepository.deleteByTokenAndUserId(deviceToken.toLowerCase(Locale.ROOT), userId);
    }
}
