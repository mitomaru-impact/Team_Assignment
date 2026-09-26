package com.reme.re_me.repository;

import com.reme.re_me.entity.ChatProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatProfileRepository extends JpaRepository<ChatProfile, Long> {
    List<ChatProfile> findAllByUserIdOrderById(Long userId);
    Optional<ChatProfile> findByIdAndUserId(Long id, Long userId);
    Optional<ChatProfile> findByUserIdAndDefaultProfileTrue(Long userId);
}
