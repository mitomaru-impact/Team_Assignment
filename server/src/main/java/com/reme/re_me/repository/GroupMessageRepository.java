package com.reme.re_me.repository;

import com.reme.re_me.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    @Query("""
            SELECT COUNT(m) FROM GroupMessage m, ChatGroupMember member
            WHERE member.groupId = m.groupId
                AND member.userId = :userId
                AND m.senderId <> :userId
                AND (member.lastReadAt IS NULL OR m.createdAt > member.lastReadAt)
                AND (m.recalled = false OR m.recalled IS NULL)
                AND (m.silent = false OR m.silent IS NULL)
            """)
    long countAllUnreadMessages(@Param("userId") Long userId);

    List<GroupMessage> findAllByGroupIdOrderByCreatedAtAscIdAsc(String groupId);
    List<GroupMessage> findAllByGroupIdOrderByCreatedAtDescIdDesc(String groupId);

    @Query("SELECT COUNT(m) FROM GroupMessage m WHERE m.groupId = :groupId AND m.senderId <> :userId")
    long countUnread(
            @Param("groupId") String groupId,
            @Param("userId") Long userId);

    @Query("""
            SELECT COUNT(m) FROM GroupMessage m
            WHERE m.groupId = :groupId AND m.senderId <> :userId AND m.createdAt > :after
            """)
    long countUnreadAfter(
            @Param("groupId") String groupId,
            @Param("userId") Long userId,
            @Param("after") LocalDateTime after);

    @Transactional
    void deleteAllByGroupId(String groupId);

    @Modifying
    @Transactional
    @Query("UPDATE GroupMessage m SET m.replyToContent = 'メッセージの送信を取り消しました', "
            + "m.replyToSenderName = null WHERE m.replyToMessageId = :messageId AND m.groupId = :groupId")
    int markRepliesToRecalledMessage(
            @Param("groupId") String groupId,
            @Param("messageId") Long messageId);
}
