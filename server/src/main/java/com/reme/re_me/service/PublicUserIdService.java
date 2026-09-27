package com.reme.re_me.service;

import com.reme.re_me.entity.User;
import com.reme.re_me.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class PublicUserIdService {

    private final UserRepository userRepository;

    public PublicUserIdService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User resolve(String publicUserId) {
        if (publicUserId == null || publicUserId.isBlank()) {
            throw new IllegalArgumentException("ユーザーIDが必要です");
        }
        return userRepository.findByPublicId(publicUserId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
    }

    public Long resolveInternalId(String publicUserId) {
        return resolve(publicUserId).getId();
    }
}
