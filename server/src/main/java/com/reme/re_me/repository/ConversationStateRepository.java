package com.reme.re_me.repository;

import com.reme.re_me.entity.ConversationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ConversationStateRepository extends JpaRepository<ConversationState, Long> {
    Optional<ConversationState> findByUserIdAndPartnerId(Long userId, Long partnerId);

    @Transactional
    void deleteByUserIdAndPartnerId(Long userId, Long partnerId);
}
