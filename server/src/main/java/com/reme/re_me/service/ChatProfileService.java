package com.reme.re_me.service;

import com.reme.re_me.dto.ChatProfileDto;
import com.reme.re_me.entity.ChatProfile;
import com.reme.re_me.entity.ConversationState;
import com.reme.re_me.entity.Message;
import com.reme.re_me.entity.ProfileContact;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.ChatProfileRepository;
import com.reme.re_me.repository.ConversationStateRepository;
import com.reme.re_me.repository.MessageRepository;
import com.reme.re_me.repository.ProfileContactRepository;
import com.reme.re_me.repository.UserRepository;
import com.reme.re_me.websocket.UserWebSocketEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ChatProfileService {

    private final ChatProfileRepository profileRepository;
    private final ProfileContactRepository contactRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationStateRepository conversationStateRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ChatProfileService(
            ChatProfileRepository profileRepository,
            ProfileContactRepository contactRepository,
            UserRepository userRepository,
            MessageRepository messageRepository,
            ConversationStateRepository conversationStateRepository,
            ApplicationEventPublisher eventPublisher) {
        this.profileRepository = profileRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.conversationStateRepository = conversationStateRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public List<ChatProfileDto> getProfiles(Long userId) {
        User user = requireUser(userId);
        ChatProfile main = profileRepository.findByUserIdAndDefaultProfileTrue(userId).orElse(null);
        if (main == null) {
            main = profileRepository.save(new ChatProfile(userId, "メイン", user.getName(), true));
            migrateExistingContacts(userId, main);
        }
        List<ChatProfile> profiles = profileRepository.findAllByUserIdOrderById(userId);
        List<Long> profilesWithLegacyIdentity = profiles.stream()
                .filter(profile -> profile.getDisplayName() == null || profile.getDisplayName().isBlank())
                .map(ChatProfile::getId)
                .toList();
        profiles.stream()
                .filter(profile -> profilesWithLegacyIdentity.contains(profile.getId()))
                .forEach(profile -> profile.setDisplayName(user.getName()));
        profileRepository.saveAll(profiles);
        profilesWithLegacyIdentity.forEach(profileId -> notifyProfileContactsAfterCommit(userId, profileId));
        return profiles.stream()
                .map(ChatProfileDto::new)
                .toList();
    }

    @Transactional
    public ChatProfileDto createProfile(Long userId, String name) {
        User user = requireUser(userId);
        if (name == null || name.isBlank() || name.trim().length() > 40) {
            throw new IllegalArgumentException("プロファイル名は1〜40文字で入力してください");
        }
        if (profileRepository.findAllByUserIdOrderById(userId).size() >= 20) {
            throw new IllegalArgumentException("プロファイルは20個まで作成できます");
        }
        return new ChatProfileDto(
                profileRepository.save(new ChatProfile(userId, name.trim(), user.getName(), false)));
    }

    @Transactional
    public ChatProfileDto updateDisplayName(Long userId, Long profileId, String displayName) {
        ChatProfile profile = findOwnedProfile(userId, profileId);
        if (displayName == null || displayName.isBlank() || displayName.trim().length() > 40) {
            throw new IllegalArgumentException("表示名は1〜40文字で入力してください");
        }
        profile.setDisplayName(displayName.trim());
        ChatProfile saved = profileRepository.save(profile);
        notifyProfileContactsAfterCommit(userId, profileId);
        return new ChatProfileDto(saved);
    }

    @Transactional
    public void deleteProfile(Long userId, Long profileId) {
        ChatProfile profile = findOwnedProfile(userId, profileId);
        if (profile.isDefaultProfile()) {
            throw new IllegalArgumentException("メインプロファイルは削除できません");
        }
        notifyProfileContactsAfterCommit(userId, profileId);
        contactRepository.deleteAllByProfileId(profileId);
        profileRepository.delete(profile);
    }

    @Transactional
    public void addContact(Long userId, Long profileId, String email) {
        findOwnedProfile(userId, profileId);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("相手のメールアドレスを入力してください");
        }
        User contact = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("登録ユーザーが見つかりません"));
        if (contact.getId().equals(userId)) {
            throw new IllegalArgumentException("自分自身は追加できません");
        }

        ProfileContact existing = contactRepository.findByOwnerUserIdAndContactUserId(userId, contact.getId())
                .orElse(null);
        ProfileContact relation = existing == null
                ? new ProfileContact(userId, contact.getId(), profileId)
                : existing;
        relation.setProfileId(profileId);
        contactRepository.save(relation);
        User owner = userRepository.findById(userId).orElseThrow();
        notifyIdentityChangedAfterCommit(contact.getId(), owner.getEmail());
    }

    @Transactional
    public void removeContact(Long userId, Long profileId, String email) {
        findOwnedProfile(userId, profileId);
        User contact = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("登録ユーザーが見つかりません"));
        contactRepository.findByOwnerUserIdAndContactUserId(userId, contact.getId())
                .filter(relation -> relation.getProfileId().equals(profileId))
                .ifPresent(relation -> {
                    contactRepository.delete(relation);
                    User owner = userRepository.findById(userId).orElseThrow();
                    notifyIdentityChangedAfterCommit(contact.getId(), owner.getEmail());
                });
    }

    @Transactional
    public void assignUnclassifiedContact(Long userId, Long profileId, Long contactUserId) {
        findOwnedProfile(userId, profileId);
        User contact = userRepository.findById(contactUserId)
                .orElseThrow(() -> new IllegalArgumentException("相手ユーザーが見つかりません"));
        if (contact.getId().equals(userId)) {
            throw new IllegalArgumentException("自分自身は追加できません");
        }
        ProfileContact relation = contactRepository.findByOwnerUserIdAndContactUserId(userId, contactUserId)
                .orElseGet(() -> new ProfileContact(userId, contactUserId, profileId));
        relation.setProfileId(profileId);
        contactRepository.save(relation);
        User owner = userRepository.findById(userId).orElseThrow();
        notifyIdentityChangedAfterCommit(contactUserId, owner.getEmail());
    }

    public String displayNameForContact(Long ownerUserId, Long contactUserId) {
        User owner = userRepository.findById(ownerUserId).orElse(null);
        if (owner == null) {
            return "Unknown";
        }
        ProfileContact contact = contactRepository
                .findByOwnerUserIdAndContactUserId(ownerUserId, contactUserId)
                .orElse(null);
        if (contact == null) {
            return owner.getEmail();
        }
        return profileRepository.findByIdAndUserId(contact.getProfileId(), ownerUserId)
                .map(ChatProfile::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(owner.getEmail());
    }

    public String displayNameForEmailContact(Long viewerUserId, String contactEmail) {
        User contact = userRepository.findByEmail(contactEmail).orElse(null);
        return contact == null
                ? contactEmail
                : displayNameForContact(contact.getId(), viewerUserId);
    }

    @Transactional
    public void deleteConversation(Long userId, Long partnerId, Long profileId, boolean deleteForBoth) {
        requireUser(userId);
        if (partnerId == null || partnerId.equals(userId) || !userRepository.existsById(partnerId)) {
            throw new IllegalArgumentException("有効な相手ユーザーが指定されていません");
        }
        if (profileId != null) {
            findOwnedProfile(userId, profileId);
            boolean belongsToProfile = contactRepository
                    .findByOwnerUserIdAndContactUserId(userId, partnerId)
                    .map(contact -> contact.getProfileId().equals(profileId))
                    .orElse(false);
            if (!belongsToProfile) {
                throw new IllegalArgumentException("相手は指定プロファイルに登録されていません");
            }
        }

        if (deleteForBoth) {
            User partner = userRepository.findById(partnerId).orElseThrow();
            User owner = userRepository.findById(userId).orElseThrow();
            messageRepository.deleteConversation(userId, partnerId);
            contactRepository.deleteByOwnerUserIdAndContactUserId(userId, partnerId);
            contactRepository.deleteByOwnerUserIdAndContactUserId(partnerId, userId);
            conversationStateRepository.deleteByUserIdAndPartnerId(userId, partnerId);
            conversationStateRepository.deleteByUserIdAndPartnerId(partnerId, userId);
            notifyConversationDeletedAfterCommit(userId, partner.getEmail(), false);
            notifyConversationDeletedAfterCommit(partnerId, owner.getEmail(), true);
            return;
        }

        if (profileId != null) {
            contactRepository.deleteByOwnerUserIdAndContactUserId(userId, partnerId);
        }
        ConversationState state = conversationStateRepository.findByUserIdAndPartnerId(userId, partnerId)
                .orElseGet(() -> new ConversationState(userId, partnerId, LocalDateTime.now()));
        state.setClearedAt(LocalDateTime.now());
        conversationStateRepository.save(state);
        User partner = userRepository.findById(partnerId).orElseThrow();
        notifyConversationDeletedAfterCommit(userId, partner.getEmail(), false);
    }

    private void notifyConversationDeletedAfterCommit(Long userId, String partnerEmail, boolean forceClose) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new UserWebSocketEvent(userId, java.util.Map.of(
                        "type", "conversation_deleted",
                        "partnerEmail", partnerEmail,
                        "forceClose", forceClose)));
            }
        });
    }

    private void notifyProfileContactsAfterCommit(Long ownerUserId, Long profileId) {
        User owner = userRepository.findById(ownerUserId).orElseThrow();
        Set<Long> contactIds = new HashSet<>();
        for (ProfileContact contact : contactRepository.findAllByOwnerUserIdAndProfileId(ownerUserId, profileId)) {
            contactIds.add(contact.getContactUserId());
        }
        for (Long contactId : contactIds) {
            notifyIdentityChangedAfterCommit(contactId, owner.getEmail());
        }
    }

    private void notifyIdentityChangedAfterCommit(Long recipientUserId, String profileOwnerEmail) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new UserWebSocketEvent(recipientUserId, java.util.Map.of(
                        "type", "profile_identity_changed",
                        "partnerEmail", profileOwnerEmail)));
            }
        });
    }

    public ChatProfile findOwnedProfile(Long userId, Long profileId) {
        return profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new IllegalArgumentException("プロファイルが見つかりません"));
    }

    private User requireUser(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new IllegalArgumentException("ユーザーが見つかりません");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
    }

    private void migrateExistingContacts(Long userId, ChatProfile main) {
        Set<Long> partners = new HashSet<>();
        for (Message message : messageRepository.findAll()) {
            if (message.getSenderId().equals(userId)) {
                partners.add(message.getReceiverId());
            } else if (message.getReceiverId().equals(userId)) {
                partners.add(message.getSenderId());
            }
        }
        for (Long partnerId : partners) {
            if (partnerId.equals(userId)
                    || contactRepository.findByOwnerUserIdAndContactUserId(userId, partnerId).isPresent()) {
                continue;
            }
            contactRepository.save(new ProfileContact(userId, partnerId, main.getId()));
        }
    }
}
