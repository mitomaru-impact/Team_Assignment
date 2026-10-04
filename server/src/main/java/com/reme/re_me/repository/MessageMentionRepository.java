package com.reme.re_me.repository;

import com.reme.re_me.entity.MessageMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageMentionRepository extends JpaRepository<MessageMention, Long> {
    List<MessageMention> findAllByGroupIdIsNullAndMessageIdIn(List<Long> messageIds);
    List<MessageMention> findAllByGroupIdAndMessageIdIn(String groupId, List<Long> messageIds);
    List<MessageMention> findAllByGroupIdIsNullAndMessageId(Long messageId);
    List<MessageMention> findAllByGroupIdAndMessageId(String groupId, Long messageId);
    void deleteAllByGroupIdIsNullAndMessageId(Long messageId);
    void deleteAllByGroupIdAndMessageId(String groupId, Long messageId);
    void deleteAllByGroupId(String groupId);
}
