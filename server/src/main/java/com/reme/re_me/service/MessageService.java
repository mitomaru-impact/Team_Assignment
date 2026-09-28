package com.reme.re_me.service;

import com.reme.re_me.dto.ConversationDto;
import com.reme.re_me.dto.MessageDto;
import com.reme.re_me.dto.SendMessageRequest;
import com.reme.re_me.entity.Message;
import com.reme.re_me.entity.ProfileContact;
import com.reme.re_me.entity.ConversationState;
import com.reme.re_me.entity.ChatProfile;
import com.reme.re_me.entity.User;
import com.reme.re_me.websocket.MessageWebSocketHandler;
import com.reme.re_me.repository.MessageRepository;
import com.reme.re_me.repository.ProfileContactRepository;
import com.reme.re_me.repository.ConversationStateRepository;
import com.reme.re_me.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.LinkedHashMap;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageWebSocketHandler messageWebSocketHandler;
    private final ApnsPushNotificationService apnsPushNotificationService;
    private final ProfileContactRepository profileContactRepository;
    private final ChatProfileService chatProfileService;
    private final ConversationStateRepository conversationStateRepository;
    private final PublicUserIdService publicUserIdService;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository,
                          MessageWebSocketHandler messageWebSocketHandler,
                          ApnsPushNotificationService apnsPushNotificationService,
                          ProfileContactRepository profileContactRepository,
                          ChatProfileService chatProfileService,
                          ConversationStateRepository conversationStateRepository,
                          PublicUserIdService publicUserIdService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.apnsPushNotificationService = apnsPushNotificationService;
        this.profileContactRepository = profileContactRepository;
        this.chatProfileService = chatProfileService;
        this.conversationStateRepository = conversationStateRepository;
        this.publicUserIdService = publicUserIdService;
    }

    // メッセージ送信処理
    public MessageDto sendMessage(SendMessageRequest request) {
        if (request.getSenderId() == null || request.getReceiverEmail() == null
                || request.getReceiverEmail().isBlank() || request.getContent() == null
                || request.getContent().isBlank()) {
            throw new IllegalArgumentException("送信者、宛先、メッセージ本文が必要です");
        }

        User sender = publicUserIdService.resolve(request.getSenderId());
        User receiver = userRepository.findByEmail(request.getReceiverEmail())
                .orElseThrow(() -> new RuntimeException("指定されたメールアドレスのユーザーが見つかりません"));
        String senderDisplayName;
        if (request.getProfileId() != null) {
            ChatProfile senderProfile = chatProfileService.findOwnedProfile(sender.getId(), request.getProfileId());
            ProfileContact contact = profileContactRepository
                    .findByOwnerUserIdAndContactUserId(sender.getId(), receiver.getId())
                    .orElseThrow(() -> new IllegalArgumentException("相手を選択中のプロファイルに追加してください"));
            if (!contact.getProfileId().equals(request.getProfileId())) {
                throw new IllegalArgumentException("相手が別のプロファイルに登録されています");
            }
            senderDisplayName = senderProfile.getDisplayName() == null
                    ? sender.getName()
                    : senderProfile.getDisplayName();
        } else {
            ProfileContact assignedContact = profileContactRepository
                    .findByOwnerUserIdAndContactUserId(sender.getId(), receiver.getId())
                    .orElse(null);
            ChatProfile senderProfile = assignedContact == null
                    ? null
                    : chatProfileService.findOwnedProfile(sender.getId(), assignedContact.getProfileId());
            senderDisplayName = senderProfile == null || senderProfile.getDisplayName() == null
                    ? sender.getName()
                    : senderProfile.getDisplayName();
        }

        Message repliedMessage = null;
        if (request.getReplyToMessageId() != null) {
            repliedMessage = messageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new IllegalArgumentException("返信先のメッセージが見つかりません"));
            boolean sameConversation =
                    (repliedMessage.getSenderId().equals(sender.getId())
                            && repliedMessage.getReceiverId().equals(receiver.getId()))
                    || (repliedMessage.getSenderId().equals(receiver.getId())
                            && repliedMessage.getReceiverId().equals(sender.getId()));
            if (!sameConversation) {
                throw new IllegalArgumentException("別の会話のメッセージには返信できません");
            }
            LocalDateTime clearedAt = getClearedAt(sender.getId(), receiver.getId());
            if (clearedAt != null && !repliedMessage.getCreatedAt().isAfter(clearedAt)) {
                throw new IllegalArgumentException("削除済みの履歴には返信できません");
            }
        }
        User repliedSender = repliedMessage == null
                ? null
                : userRepository.findById(repliedMessage.getSenderId()).orElse(null);
        Message message = new Message(
                sender.getId(),
                receiver.getId(),
                request.getContent(),
                senderDisplayName,
                repliedMessage == null ? null : repliedMessage.getId(),
                repliedMessage == null ? null : repliedMessage.getContent(),
                repliedMessage == null
                        ? null
                        : repliedMessage.getSenderDisplayName() != null
                                ? repliedMessage.getSenderDisplayName()
                                : repliedSender == null ? null : repliedSender.getName());
        Message saved = messageRepository.save(message);
        logger.info("Saved message {} from user {} to user {}",
                saved.getId(), saved.getSenderId(), saved.getReceiverId());

        MessageDto messageDto = new MessageDto(
                saved,
                sender.getPublicId(),
                sender.getEmail(),
                receiver.getPublicId(),
                chatProfileService.displayNameForContact(sender.getId(), receiver.getId()));
        boolean unclassifiedForReceiver = profileContactRepository
                .findByOwnerUserIdAndContactUserId(receiver.getId(), sender.getId())
                .isEmpty();
        messageDto.setNotificationMetadata(
                unclassifiedForReceiver,
                unclassifiedForReceiver ? "未分類" : senderDisplayName,
                unclassifiedForReceiver ? "未分類に新しいメッセージがあります。" : saved.getContent());
        messageWebSocketHandler.sendToUser(saved.getReceiverId(), messageDto);
        if (!saved.getSenderId().equals(saved.getReceiverId())) {
            messageWebSocketHandler.sendToUser(saved.getSenderId(), messageDto);
            apnsPushNotificationService.sendNewMessage(
                    saved.getReceiverId(), saved.getId(), saved.getContent(),
                    senderDisplayName, sender.getEmail());
        } else {
            logger.warn("Skipping APNs notification for self-addressed message {}", saved.getId());
        }
        return messageDto;
    }

    @Transactional
    public MessageDto editMessage(Long userId, Long messageId, String content) {
        if (content == null || content.isBlank() || content.trim().length() > 1000) {
            throw new IllegalArgumentException("メッセージは1〜1000文字で入力してください");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("メッセージが見つかりません"));
        if (!message.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("自分のメッセージだけ編集できます");
        }
        if (!"MESSAGE".equals(message.getMessageType())) {
            throw new IllegalArgumentException("このメッセージは編集できません");
        }
        message.setContent(content.trim());
        Message saved = messageRepository.save(message);
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("送信者が見つかりません"));
        User receiver = userRepository.findById(saved.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("受信者が見つかりません"));
        MessageDto dto = new MessageDto(
                saved,
                sender.getPublicId(),
                sender.getEmail(),
                receiver.getPublicId(),
                chatProfileService.displayNameForContact(userId, receiver.getId()));
        messageWebSocketHandler.sendToUser(saved.getReceiverId(), dto);
        messageWebSocketHandler.sendToUser(saved.getSenderId(), dto);
        return dto;
    }

    // チャット履歴取得処理
    public List<MessageDto> getChatHistory(Long userId, String targetEmail) {
        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("相手ユーザーが見つかりません"));

        List<Message> history = messageRepository.findChatHistory(userId, targetUser.getId());
        LocalDateTime clearedAt = getClearedAt(userId, targetUser.getId());
        if (clearedAt != null) {
            history = history.stream()
                    .filter(message -> message.getCreatedAt().isAfter(clearedAt))
                    .toList();
        }
        Set<Long> participantIds = history.stream()
                .flatMap(message -> java.util.stream.Stream.of(message.getSenderId(), message.getReceiverId()))
                .collect(Collectors.toSet());
        Map<Long, String> publicIds = userRepository.findAllById(participantIds).stream()
                .collect(Collectors.toMap(User::getId, User::getPublicId));
        return history.stream()
                .map(message -> new MessageDto(
                        message,
                        publicIds.get(message.getSenderId()),
                            userRepository.findById(message.getSenderId()).map(User::getEmail).orElse(null),
                            publicIds.get(message.getReceiverId()),
                            chatProfileService.displayNameForContact(message.getSenderId(), message.getReceiverId())))
                .collect(Collectors.toList());
    }
    
    // 会話相手の一覧を取得
    public List<ConversationDto> getConversations(Long userId, Long profileId, boolean unclassified) {
        if (unclassified) {
            return getUnclassifiedConversations(userId);
        }
        chatProfileService.findOwnedProfile(userId, profileId);
        Map<Long, ProfileContact> contacts = profileContactRepository
                .findAllByOwnerUserIdAndProfileId(userId, profileId).stream()
                .collect(Collectors.toMap(ProfileContact::getContactUserId, contact -> contact));

        // ユーザーが関与したすべてのメッセージを取得
        List<Message> allMessages = messageRepository.findAll().stream()
                .filter(m -> (m.getSenderId().equals(userId) && contacts.containsKey(m.getReceiverId()))
                        || (m.getReceiverId().equals(userId) && contacts.containsKey(m.getSenderId())))
                .filter(m -> {
                    Long partnerId = m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId();
                    LocalDateTime clearedAt = getClearedAt(userId, partnerId);
                    return clearedAt == null || m.getCreatedAt().isAfter(clearedAt);
                })
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt())) // 新しい順
                .toList();

        Map<Long, Message> latestMessagePerPartner = new LinkedHashMap<>();
        for (Message m : allMessages) {
            Long partnerId = m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId();
            latestMessagePerPartner.putIfAbsent(partnerId, m);
        }

        return contacts.keySet().stream()
                .map(partnerId -> toConversation(
                        userId,
                        partnerId,
                        latestMessagePerPartner.get(partnerId)))
                .collect(Collectors.toList());
    }

    public List<ConversationDto> getUnclassifiedConversations(Long userId) {
        List<Long> assignedContacts = profileContactRepository.findAllByOwnerUserId(userId).stream()
                .map(contact -> contact.getContactUserId())
                .toList();
        List<Message> allMessages = messageRepository.findAll().stream()
                .filter(message -> message.getReceiverId().equals(userId)
                        && !assignedContacts.contains(message.getSenderId()))
                .filter(message -> {
                    LocalDateTime clearedAt = getClearedAt(userId, message.getSenderId());
                    return clearedAt == null || message.getCreatedAt().isAfter(clearedAt);
                })
                .sorted((first, second) -> second.getCreatedAt().compareTo(first.getCreatedAt()))
                .toList();
        Map<Long, Message> latestBySender = new LinkedHashMap<>();
        for (Message message : allMessages) {
            latestBySender.putIfAbsent(message.getSenderId(), message);
        }
        return latestBySender.entrySet().stream()
                .map(entry -> toConversation(userId, entry.getKey(), entry.getValue(), true))
                .collect(Collectors.toList());
    }

    private ConversationDto toConversation(
            Long userId,
            Long partnerId,
            Message lastMessage) {
        return toConversation(userId, partnerId, lastMessage, false);
    }

    private ConversationDto toConversation(
            Long userId,
            Long partnerId,
            Message lastMessage,
            boolean unclassified) {
        User partner = userRepository.findById(partnerId).orElse(null);
        String partnerEmail = partner == null ? "Unknown" : partner.getEmail();
        String partnerName = unclassified
                ? partnerEmail
                : partner == null
                        ? partnerEmail
                        : chatProfileService.displayNameForContact(partnerId, userId);
        LocalDateTime clearedAt = getClearedAt(userId, partnerId);
        long unreadCount = clearedAt == null
                ? messageRepository.countUnreadMessages(userId, partnerId)
                : messageRepository.countUnreadMessagesAfter(userId, partnerId, clearedAt);
        return new ConversationDto(
                partnerEmail,
                partner == null ? null : partner.getPublicId(),
                partnerName,
                lastMessage == null ? "" : lastMessage.getContent(),
                lastMessage == null ? null : lastMessage.getCreatedAt(),
                unreadCount);
    }

    private LocalDateTime getClearedAt(Long userId, Long partnerId) {
        return conversationStateRepository.findByUserIdAndPartnerId(userId, partnerId)
                .map(ConversationState::getClearedAt)
                .orElse(null);
    }

    public void markConversationAsRead(Long userId, String targetEmail) {
        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("相手ユーザーが見つかりません"));
        messageRepository.markConversationAsRead(userId, targetUser.getId());
    }
}