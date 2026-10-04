package com.reme.re_me.repository;

import com.reme.re_me.entity.ConversationMute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationMuteRepository extends JpaRepository<ConversationMute, Long> {
    Optional<ConversationMute> findByUserIdAndPartnerId(Long userId, Long partnerId);
    Optional<ConversationMute> findByUserIdAndGroupId(Long userId, String groupId);
    List<ConversationMute> findAllByUserId(Long userId);
    void deleteByUserIdAndPartnerId(Long userId, Long partnerId);
    void deleteByUserIdAndGroupId(Long userId, String groupId);
}
