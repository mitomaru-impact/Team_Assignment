package com.reme.re_me.service;

import com.reme.re_me.entity.GroupMessage;
import com.reme.re_me.entity.Message;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.ChatGroupMemberRepository;
import com.reme.re_me.repository.GroupMessageRepository;
import com.reme.re_me.repository.MessageReactionRepository;
import com.reme.re_me.repository.MessageRepository;
import com.reme.re_me.repository.UserRepository;
import com.reme.re_me.websocket.UserWebSocketEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@Service
public class MessageRecallService {

    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final MessageReactionRepository reactionRepository;
    private final ChatGroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageMentionService mentionService;

    public MessageRecallService(
            MessageRepository messageRepository,
            GroupMessageRepository groupMessageRepository,
            MessageReactionRepository reactionRepository,
            ChatGroupMemberRepository memberRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            MessageMentionService mentionService) {
        this.messageRepository = messageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.reactionRepository = reactionRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.mentionService = mentionService;
    }

    @Transactional
    public void recallDirect(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("メッセージが見つかりません"));
        if (!message.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("自分が送信したメッセージのみ取り消せます");
        }
        if (!"MESSAGE".equals(message.getMessageType())) {
            throw new IllegalArgumentException("このメッセージは取り消せません");
        }

        message.recall();
        messageRepository.save(message);
        messageRepository.markRepliesToRecalledMessage(messageId);
        reactionRepository.deleteAllByMessageIds(List.of(messageId));
        mentionService.deleteForMessage(messageId, null);

        User sender = userRepository.findById(message.getSenderId())
                .orElseThrow(() -> new IllegalStateException("送信者が見つかりません"));
        User receiver = userRepository.findById(message.getReceiverId())
                .orElseThrow(() -> new IllegalStateException("受信者が見つかりません"));
        afterCommit(() -> {
            publishDirectRecall(sender, receiver, messageId);
            if (!sender.getId().equals(receiver.getId())) {
                publishDirectRecall(receiver, sender, messageId);
            }
        });
    }

    @Transactional
    public void recallGroup(Long userId, String groupId, Long messageId) {
        if (memberRepository.findByGroupIdAndUserId(groupId, userId).isEmpty()) {
            throw new IllegalArgumentException("グループのメンバーではありません");
        }
        GroupMessage message = groupMessageRepository.findById(messageId)
                .filter(candidate -> candidate.getGroupId().equals(groupId))
                .orElseThrow(() -> new IllegalArgumentException("メッセージが見つかりません"));
        if (!message.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("自分が送信したメッセージのみ取り消せます");
        }
        if (Boolean.TRUE.equals(message.getRecalled())) {
            throw new IllegalArgumentException("このメッセージはすでに取り消されています");
        }

        message.recall();
        groupMessageRepository.save(message);
        groupMessageRepository.markRepliesToRecalledMessage(groupId, messageId);
        reactionRepository.deleteAllByGroupMessageIds(List.of(messageId));
        mentionService.deleteForMessage(messageId, groupId);

        List<Long> memberIds = memberRepository.findAllByGroupId(groupId).stream()
                .map(member -> member.getUserId())
                .toList();
        afterCommit(() -> memberIds.forEach(memberId -> eventPublisher.publishEvent(
                new UserWebSocketEvent(memberId, Map.of(
                        "type", "message_recalled",
                        "messageId", messageId,
                        "groupId", groupId)))));
    }

    private void publishDirectRecall(User recipient, User partner, Long messageId) {
        eventPublisher.publishEvent(new UserWebSocketEvent(recipient.getId(), Map.of(
                "type", "message_recalled",
                "messageId", messageId,
                "partnerEmail", partner.getEmail())));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
