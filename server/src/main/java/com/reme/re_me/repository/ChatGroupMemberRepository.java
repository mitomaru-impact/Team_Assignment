package com.reme.re_me.repository;

import com.reme.re_me.entity.ChatGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface ChatGroupMemberRepository extends JpaRepository<ChatGroupMember, Long> {
    List<ChatGroupMember> findAllByGroupId(String groupId);
    List<ChatGroupMember> findAllByUserId(Long userId);
    Optional<ChatGroupMember> findByGroupIdAndUserId(String groupId, Long userId);

    @Transactional
    void deleteAllByGroupId(String groupId);

    @Transactional
    void deleteByGroupIdAndUserId(String groupId, Long userId);
}
