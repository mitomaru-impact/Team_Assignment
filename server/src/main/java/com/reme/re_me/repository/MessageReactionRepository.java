package com.reme.re_me.repository;

import com.reme.re_me.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    List<MessageReaction> findAllByMessageIdIn(List<Long> messageIds);
    List<MessageReaction> findAllByGroupMessageIdIn(List<Long> messageIds);
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);
    Optional<MessageReaction> findByGroupMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    @Modifying
    @Query("DELETE FROM MessageReaction r WHERE r.messageId IN :messageIds")
    int deleteAllByMessageIds(@Param("messageIds") List<Long> messageIds);

    @Modifying
    @Query("DELETE FROM MessageReaction r WHERE r.groupMessageId IN :messageIds")
    int deleteAllByGroupMessageIds(@Param("messageIds") List<Long> messageIds);
}
