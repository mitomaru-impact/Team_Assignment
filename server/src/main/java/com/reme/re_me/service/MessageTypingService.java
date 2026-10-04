package com.reme.re_me.service;

import com.reme.re_me.dto.TypingSignalRequest;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.ChatGroupMemberRepository;
import com.reme.re_me.repository.MessageRepository;
import com.reme.re_me.repository.ProfileContactRepository;
import com.reme.re_me.repository.UserRepository;
import com.reme.re_me.websocket.UserWebSocketEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MessageTypingService {

    private final UserRepository userRepository;
    private final ProfileContactRepository profileContactRepository;
    private final MessageRepository messageRepository;
    private final ChatGroupMemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MessageTypingService(
            UserRepository userRepository,
            ProfileContactRepository profileContactRepository,
            MessageRepository messageRepository,
            ChatGroupMemberRepository memberRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.profileContactRepository = profileContactRepository;
        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
    }

    public void relay(Long senderId, TypingSignalRequest signal) {
        if (signal.isTyping() == null) {
            throw new IllegalArgumentException("入力状態が必要です");
        }
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));

        if (signal.groupId() != null && !signal.groupId().isBlank()) {
            relayToGroup(sender, signal.groupId(), signal.isTyping());
        } else {
            relayToContact(sender, signal.targetEmail(), signal.isTyping());
        }
    }

    private void relayToContact(User sender, String targetEmail, boolean isTyping) {
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalArgumentException("相手が指定されていません");
        }
        User target = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new IllegalArgumentException("相手ユーザーが見つかりません"));
        if (target.getId().equals(sender.getId())
                || (profileContactRepository.findByOwnerUserIdAndContactUserId(sender.getId(), target.getId()).isEmpty()
                && profileContactRepository.findByOwnerUserIdAndContactUserId(target.getId(), sender.getId()).isEmpty()
                && messageRepository.findChatHistory(sender.getId(), target.getId()).isEmpty())) {
            throw new IllegalArgumentException("この相手には入力状態を送信できません");
        }

        eventPublisher.publishEvent(new UserWebSocketEvent(target.getId(), Map.of(
                "type", "typing",
                "senderId", sender.getPublicId(),
                "partnerEmail", sender.getEmail(),
                "isTyping", isTyping)));
    }

    private void relayToGroup(User sender, String groupId, boolean isTyping) {
        if (memberRepository.findByGroupIdAndUserId(groupId, sender.getId()).isEmpty()) {
            throw new IllegalArgumentException("グループのメンバーではありません");
        }
        List<Long> memberIds = memberRepository.findAllByGroupId(groupId).stream()
                .map(member -> member.getUserId())
                .filter(userId -> !userId.equals(sender.getId()))
                .toList();
        Map<String, Object> event = Map.of(
                "type", "typing",
                "senderId", sender.getPublicId(),
                "groupId", groupId,
                "isTyping", isTyping);
        memberIds.forEach(memberId -> eventPublisher.publishEvent(new UserWebSocketEvent(memberId, event)));
    }
}
