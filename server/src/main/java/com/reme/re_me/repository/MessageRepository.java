package com.reme.re_me.repository;

import com.reme.re_me.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    
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

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :receiverId AND m.senderId = :senderId AND m.createdAt > :after AND (m.readStatus = false OR m.readStatus IS NULL)")
    long countUnreadMessagesAfter(
            @Param("receiverId") Long receiverId,
            @Param("senderId") Long senderId,
            @Param("after") java.time.LocalDateTime after);
}