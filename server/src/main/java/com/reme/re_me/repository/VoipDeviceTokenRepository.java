package com.reme.re_me.repository;

import com.reme.re_me.entity.VoipDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface VoipDeviceTokenRepository extends JpaRepository<VoipDeviceToken, Long> {
    List<VoipDeviceToken> findAllByUserId(Long userId);
    Optional<VoipDeviceToken> findByToken(String token);

    @Transactional
    void deleteByToken(String token);

    @Transactional
    void deleteByTokenAndUserId(String token, Long userId);
}
