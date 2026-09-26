package com.reme.re_me.service;

import com.reme.re_me.dto.ConversationDto;
import com.reme.re_me.dto.MessageDto;
import com.reme.re_me.dto.SendMessageRequest;
import com.reme.re_me.entity.Message;
import com.reme.re_me.entity.ProfileContact;
import com.reme.re_me.entity.User;
import com.reme.re_me.websocket.MessageWebSocketHandler;
import com.reme.re_me.repository.MessageRepository;
import com.reme.re_me.repository.ProfileContactRepository;
import com.reme.re_me.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.LinkedHashMap;

import java.util.List;
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

    public MessageService(MessageRepository messageRepository, UserRepository userRepository,
                          MessageWebSocketHandler messageWebSocketHandler,
                          ApnsPushNotificationService apnsPushNotificationService,
                          ProfileContactRepository profileContactRepository,
                          ChatProfileService chatProfileService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.apnsPushNotificationService = apnsPushNotificationService;
        this.profileContactRepository = profileContactRepository;
        this.chatProfileService = chatProfileService;
    }

    // メッセージ送信処理
    public MessageDto sendMessage(SendMessageRequest request) {
        if (request.getSenderId() == null || request.getReceiverEmail() == null
                || request.getReceiverEmail().isBlank() || request.getContent() == null
                || request.getContent().isBlank()) {
            throw new IllegalArgumentException("送信者、宛先、メッセージ本文が必要です");
        }
        User receiver = userRepository.findByEmail(request.getReceiverEmail())
                .orElseThrow(() -> new RuntimeException("指定されたメールアドレスのユーザーが見つかりません"));
        if (request.getProfileId() != null) {
            chatProfileService.findOwnedProfile(request.getSenderId(), request.getProfileId());
            ProfileContact contact = profileContactRepository
                    .findByOwnerUserIdAndContactUserId(request.getSenderId(), receiver.getId())
                    .orElseThrow(() -> new IllegalArgumentException("相手を選択中のプロフィールに追加してください"));
            if (!contact.getProfileId().equals(request.getProfileId())) {
                throw new IllegalArgumentException("相手が別のプロフィールに登録されています");
            }
        }

        Message message = new Message(request.getSenderId(), receiver.getId(), request.getContent());
        Message saved = messageRepository.save(message);
        logger.info("Saved message {} from user {} to user {}",
                saved.getId(), saved.getSenderId(), saved.getReceiverId());

        MessageDto messageDto = new MessageDto(saved);
        messageWebSocketHandler.sendToUser(saved.getReceiverId(), messageDto);
        if (!saved.getSenderId().equals(saved.getReceiverId())) {
            messageWebSocketHandler.sendToUser(saved.getSenderId(), messageDto);
            User sender = userRepository.findById(saved.getSenderId()).orElse(null);
            apnsPushNotificationService.sendNewMessage(
                    saved.getReceiverId(), saved.getId(), saved.getContent(),
                    sender == null ? "" : sender.getEmail());
        } else {
            logger.warn("Skipping APNs notification for self-addressed message {}", saved.getId());
        }
        return messageDto;
    }

    // チャット履歴取得処理
    public List<MessageDto> getChatHistory(Long userId, String targetEmail) {
        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("相手ユーザーが見つかりません"));

        List<Message> history = messageRepository.findChatHistory(userId, targetUser.getId());
        return history.stream().map(MessageDto::new).collect(Collectors.toList());
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
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt())) // 新しい順
                .toList();

        Map<Long, Message> latestMessagePerPartner = new LinkedHashMap<>();

        for (Message m : allMessages) {
            Long partnerId = m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId();
            latestMessagePerPartner.putIfAbsent(partnerId, m);
        }

        return contacts.keySet().stream()
                .map(partnerId -> toConversation(userId, partnerId, latestMessagePerPartner.get(partnerId)))
                .collect(Collectors.toList());
    }

    public List<ConversationDto> getUnclassifiedConversations(Long userId) {
        List<Long> assignedContacts = profileContactRepository.findAllByOwnerUserId(userId).stream()
                .map(contact -> contact.getContactUserId())
                .toList();
        List<Message> allMessages = messageRepository.findAll().stream()
                .filter(message -> message.getReceiverId().equals(userId)
                        && !assignedContacts.contains(message.getSenderId()))
                .sorted((first, second) -> second.getCreatedAt().compareTo(first.getCreatedAt()))
                .toList();
        Map<Long, Message> latestBySender = new LinkedHashMap<>();
        for (Message message : allMessages) {
            latestBySender.putIfAbsent(message.getSenderId(), message);
        }
        return latestBySender.entrySet().stream()
                .map(entry -> toConversation(userId, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private ConversationDto toConversation(Long userId, Long partnerId, Message lastMessage) {
        User partner = userRepository.findById(partnerId).orElse(null);
        String partnerEmail = partner == null ? "Unknown" : partner.getEmail();
        String partnerName = partner == null ? partnerEmail : partner.getName();
        long unreadCount = messageRepository.countUnreadMessages(userId, partnerId);
        return new ConversationDto(
                partnerEmail,
                partnerId,
                partnerName,
                lastMessage == null ? "" : lastMessage.getContent(),
                lastMessage == null ? null : lastMessage.getCreatedAt(),
                unreadCount);
    }

    public void markConversationAsRead(Long userId, String targetEmail) {
        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("相手ユーザーが見つかりません"));
        messageRepository.markConversationAsRead(userId, targetUser.getId());
    }
}