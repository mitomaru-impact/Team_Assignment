package com.reme.re_me.service;

import com.reme.re_me.dto.ChatProfileDto;
import com.reme.re_me.entity.ChatProfile;
import com.reme.re_me.entity.Message;
import com.reme.re_me.entity.ProfileContact;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.ChatProfileRepository;
import com.reme.re_me.repository.MessageRepository;
import com.reme.re_me.repository.ProfileContactRepository;
import com.reme.re_me.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ChatProfileService {

    private final ChatProfileRepository profileRepository;
    private final ProfileContactRepository contactRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public ChatProfileService(
            ChatProfileRepository profileRepository,
            ProfileContactRepository contactRepository,
            UserRepository userRepository,
            MessageRepository messageRepository) {
        this.profileRepository = profileRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public List<ChatProfileDto> getProfiles(Long userId) {
        requireUser(userId);
        ChatProfile main = profileRepository.findByUserIdAndDefaultProfileTrue(userId).orElse(null);
        if (main == null) {
            main = profileRepository.save(new ChatProfile(userId, "メイン", true));
            migrateExistingContacts(userId, main);
        }
        return profileRepository.findAllByUserIdOrderById(userId).stream()
                .map(ChatProfileDto::new)
                .toList();
    }

    @Transactional
    public ChatProfileDto createProfile(Long userId, String name) {
        requireUser(userId);
        if (name == null || name.isBlank() || name.trim().length() > 40) {
            throw new IllegalArgumentException("プロフィール名は1〜40文字で入力してください");
        }
        if (profileRepository.findAllByUserIdOrderById(userId).size() >= 20) {
            throw new IllegalArgumentException("プロフィールは20個まで作成できます");
        }
        return new ChatProfileDto(profileRepository.save(new ChatProfile(userId, name.trim(), false)));
    }

    @Transactional
    public void deleteProfile(Long userId, Long profileId) {
        ChatProfile profile = findOwnedProfile(userId, profileId);
        if (profile.isDefaultProfile()) {
            throw new IllegalArgumentException("メインプロフィールは削除できません");
        }
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

        ProfileContact relation = contactRepository.findByOwnerUserIdAndContactUserId(userId, contact.getId())
                .orElseGet(() -> new ProfileContact(userId, contact.getId(), profileId));
        relation.setProfileId(profileId);
        contactRepository.save(relation);
    }

    @Transactional
    public void removeContact(Long userId, Long profileId, String email) {
        findOwnedProfile(userId, profileId);
        User contact = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("登録ユーザーが見つかりません"));
        contactRepository.findByOwnerUserIdAndContactUserId(userId, contact.getId())
                .filter(relation -> relation.getProfileId().equals(profileId))
                .ifPresent(contactRepository::delete);
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
    }

    public ChatProfile findOwnedProfile(Long userId, Long profileId) {
        return profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new IllegalArgumentException("プロフィールが見つかりません"));
    }

    private void requireUser(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new IllegalArgumentException("ユーザーが見つかりません");
        }
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
