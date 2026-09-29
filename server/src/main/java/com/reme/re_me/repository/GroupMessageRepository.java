package com.reme.re_me.repository;

import com.reme.re_me.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
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
}
