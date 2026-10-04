package com.reme.re_me.service;

import com.reme.re_me.entity.ChatGroup;
import com.reme.re_me.entity.ChatGroupMember;
import com.reme.re_me.entity.GroupMessage;
import com.reme.re_me.entity.Message;
import com.reme.re_me.entity.MessageReaction;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.ChatGroupMemberRepository;
import com.reme.re_me.repository.ChatGroupRepository;
import com.reme.re_me.repository.GroupMessageRepository;
import com.reme.re_me.repository.MessageReactionRepository;
import com.reme.re_me.repository.MessageRepository;
import com.reme.re_me.repository.UserRepository;
import com.reme.re_me.dto.MessageReactionDto;
import com.reme.re_me.websocket.UserWebSocketEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageReactionService {

    private static final Set<String> ALLOWED_EMOJIS = Set.of("👍", "❤️", "😂", "😮", "😢", "🙏");

    private final MessageReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final ChatGroupRepository groupRepository;
    private final ChatGroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MessageReactionService(
            MessageReactionRepository reactionRepository,
            MessageRepository messageRepository,
            GroupMessageRepository groupMessageRepository,
            ChatGroupRepository groupRepository,
            ChatGroupMemberRepository memberRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this.reactionRepository = reactionRepository;
        this.messageRepository = messageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<MessageReactionDto>> forDirectMessagesById(List<Message> messages, Long viewerId) {
        if (messages.isEmpty()) {
            return Map.of();
        }
        List<MessageReaction> reactions = reactionRepository.findAllByMessageIdIn(
                messages.stream().map(Message::getId).toList());
        return aggregate(reactions, viewerId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<MessageReactionDto>> forGroupMessagesById(List<GroupMessage> messages, Long viewerId) {
        if (messages.isEmpty()) {
            return Map.of();
        }
        List<MessageReaction> reactions = reactionRepository.findAllByGroupMessageIdIn(
                messages.stream().map(GroupMessage::getId).toList());
        return aggregate(reactions, viewerId);
    }

    @Transactional
    public void toggleDirect(Long userId, Long messageId, String emoji) {
        validateEmoji(emoji);
        Message message = messageRepository.findById(messageId)
                .filter(candidate -> "MESSAGE".equals(candidate.getMessageType()))
                .orElseThrow(() -> new IllegalArgumentException("メッセージが見つかりません"));
        if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("このメッセージにリアクションできません");
        }

        var existing = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            reactionRepository.save(new MessageReaction(messageId, null, userId, emoji));
        }

        User sender = userRepository.findById(message.getSenderId()).orElseThrow();
        User receiver = userRepository.findById(message.getReceiverId()).orElseThrow();
        afterCommit(() -> {
            eventPublisher.publishEvent(new UserWebSocketEvent(sender.getId(), Map.of(
                    "type", "message_reaction_changed", "partnerEmail", receiver.getEmail())));
            if (!sender.getId().equals(receiver.getId())) {
                eventPublisher.publishEvent(new UserWebSocketEvent(receiver.getId(), Map.of(
                        "type", "message_reaction_changed", "partnerEmail", sender.getEmail())));
            }
        });
    }

    @Transactional
    public void toggleGroup(Long userId, String groupId, Long messageId, String emoji) {
        validateEmoji(emoji);
        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("グループが見つかりません"));
        List<ChatGroupMember> members = memberRepository.findAllByGroupId(groupId);
        if (members.stream().noneMatch(member -> member.getUserId().equals(userId))) {
            throw new IllegalArgumentException("グループのメンバーではありません");
        }
        GroupMessage message = groupMessageRepository.findById(messageId)
                .filter(candidate -> candidate.getGroupId().equals(groupId))
                .orElseThrow(() -> new IllegalArgumentException("メッセージが見つかりません"));
        if (Boolean.TRUE.equals(message.getRecalled())) {
            throw new IllegalArgumentException("取り消し済みメッセージにはリアクションできません");
        }

        var existing = reactionRepository.findByGroupMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            reactionRepository.save(new MessageReaction(null, messageId, userId, emoji));
        }

        List<Long> memberIds = members.stream().map(ChatGroupMember::getUserId).toList();
        afterCommit(() -> memberIds.forEach(memberId -> eventPublisher.publishEvent(
                new UserWebSocketEvent(memberId, Map.of(
                        "type", "message_reaction_changed",
                        "groupId", group.getId())))));
    }

    public void deleteForDirectMessages(List<Long> messageIds) {
        if (!messageIds.isEmpty()) {
            reactionRepository.deleteAllByMessageIds(messageIds);
        }
    }

    public void deleteForGroupMessages(List<Long> messageIds) {
        if (!messageIds.isEmpty()) {
            reactionRepository.deleteAllByGroupMessageIds(messageIds);
        }
    }

    private Map<Long, List<MessageReactionDto>> aggregate(List<MessageReaction> reactions, Long viewerId) {
        Map<Long, List<MessageReaction>> byMessage = reactions.stream()
                .collect(Collectors.groupingBy(
                        reaction -> reaction.getMessageId() == null
                                ? reaction.getGroupMessageId() : reaction.getMessageId()));
        return byMessage.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                        .collect(Collectors.groupingBy(
                                MessageReaction::getEmoji,
                                Collectors.toList()))
                        .entrySet().stream()
                        .map(group -> new MessageReactionDto(
                                group.getKey(),
                                group.getValue().size(),
                                group.getValue().stream().anyMatch(
                                        reaction -> reaction.getUserId().equals(viewerId))))
                        .sorted(Comparator.comparing(MessageReactionDto::emoji))
                        .toList()));
    }

    private void validateEmoji(String emoji) {
        if (emoji == null || !ALLOWED_EMOJIS.contains(emoji)) {
            throw new IllegalArgumentException("選択できないリアクションです");
        }
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
