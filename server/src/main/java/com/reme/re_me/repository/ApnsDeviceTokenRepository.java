package com.reme.re_me.repository;

import com.reme.re_me.entity.ApnsDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ApnsDeviceTokenRepository extends JpaRepository<ApnsDeviceToken, Long> {
    List<ApnsDeviceToken> findAllByUserId(Long userId);
    Optional<ApnsDeviceToken> findByToken(String token);
    void deleteByTokenAndUserId(String token, Long userId);

    @Transactional
    void deleteByToken(String token);
}
