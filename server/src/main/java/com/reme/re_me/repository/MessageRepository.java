package com.reme.re_me.repository;

import com.reme.re_me.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT COUNT(m) FROM Message m
            LEFT JOIN ConversationState state
                ON state.userId = :userId AND state.partnerId = m.senderId
            WHERE m.receiverId = :userId
                AND m.messageType = 'MESSAGE'
                AND (m.readStatus = false OR m.readStatus IS NULL)
                AND (m.silent = false OR m.silent IS NULL)
                AND (state.id IS NULL OR m.createdAt > state.clearedAt)
            """)
    long countAllUnreadMessages(@Param("userId") Long userId);
    
    // 2人のユーザー間におけるすべてのメッセージを取得（送信日時昇順）
    @Query("SELECT m FROM Message m WHERE (m.senderId = :user1 AND m.receiverId = :user2) OR (m.senderId = :user2 AND m.receiverId = :user1) ORDER BY m.createdAt ASC")
    List<Message> findChatHistory(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :receiverId AND m.senderId = :senderId AND (m.readStatus = false OR m.readStatus IS NULL)")
    long countUnreadMessages(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    @Query("SELECT m.id FROM Message m WHERE m.receiverId = :receiverId AND m.senderId = :senderId AND m.messageType = 'MESSAGE' AND (m.readStatus = false OR m.readStatus IS NULL)")
    List<Long> findUnreadMessageIds(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.readStatus = true WHERE m.receiverId = :receiverId AND m.senderId = :senderId AND (m.readStatus = false OR m.readStatus IS NULL)")
    int markConversationAsRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE (m.senderId = :firstUser AND m.receiverId = :secondUser) OR (m.senderId = :secondUser AND m.receiverId = :firstUser)")
    int deleteConversation(@Param("firstUser") Long firstUser, @Param("secondUser") Long secondUser);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.replyToContent = 'メッセージの送信を取り消しました', m.replyToSenderName = null "
            + "WHERE m.replyToMessageId = :messageId")
    int markRepliesToRecalledMessage(@Param("messageId") Long messageId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :receiverId AND m.senderId = :senderId AND m.createdAt > :after AND (m.readStatus = false OR m.readStatus IS NULL)")
    long countUnreadMessagesAfter(
            @Param("receiverId") Long receiverId,
            @Param("senderId") Long senderId,
            @Param("after") java.time.LocalDateTime after);
}