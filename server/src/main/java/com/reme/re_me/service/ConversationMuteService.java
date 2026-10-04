package com.reme.re_me.service;

import com.reme.re_me.dto.ConversationMuteDto;
import com.reme.re_me.dto.ConversationMuteRequest;
import com.reme.re_me.entity.ConversationMute;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.ChatGroupMemberRepository;
import com.reme.re_me.repository.ConversationMuteRepository;
import com.reme.re_me.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ConversationMuteService {

    private static final Set<String> MODES = Set.of("NONE", "MENTIONS_ONLY", "ALL");

    private final ConversationMuteRepository muteRepository;
    private final UserRepository userRepository;
    private final ChatGroupMemberRepository groupMemberRepository;
    private final PublicUserIdService publicUserIdService;

    public ConversationMuteService(
            ConversationMuteRepository muteRepository,
            UserRepository userRepository,
            ChatGroupMemberRepository groupMemberRepository,
            PublicUserIdService publicUserIdService) {
        this.muteRepository = muteRepository;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.publicUserIdService = publicUserIdService;
    }

    @Transactional(readOnly = true)
    public List<ConversationMuteDto> list(String publicUserId) {
        Long userId = publicUserIdService.resolveInternalId(publicUserId);
        return muteRepository.findAllByUserId(userId).stream()
                .map(mute -> {
                    String email = mute.getPartnerUserId() == null
                            ? null
                            : userRepository.findById(mute.getPartnerUserId())
                                    .map(User::getEmail).orElse(null);
                    return new ConversationMuteDto(email, mute.getGroupId(), mute.getMode());
                })
                .toList();
    }

    @Transactional
    public void update(ConversationMuteRequest request) {
        if (request == null || request.userId() == null) {
            throw new IllegalArgumentException("ユーザーが必要です");
        }
        Long userId = publicUserIdService.resolveInternalId(request.userId());
        boolean hasEmail = request.targetEmail() != null && !request.targetEmail().isBlank();
        boolean hasGroup = request.groupId() != null && !request.groupId().isBlank();
        if (hasEmail == hasGroup) {
            throw new IllegalArgumentException("会話相手またはグループを指定してください");
        }
        String mode = request.mode() == null ? "" : request.mode().toUpperCase(Locale.ROOT);
        if (!MODES.contains(mode)) {
            throw new IllegalArgumentException("ミュート設定が正しくありません");
        }

        if (hasEmail) {
            User partner = userRepository.findByEmail(request.targetEmail().trim())
                    .orElseThrow(() -> new IllegalArgumentException("相手が見つかりません"));
            if (mode.equals("NONE")) {
                muteRepository.deleteByUserIdAndPartnerId(userId, partner.getId());
                return;
            }
            ConversationMute mute = muteRepository.findByUserIdAndPartnerId(userId, partner.getId())
                    .orElseGet(() -> new ConversationMute(userId, partner.getId(), null, mode));
            mute.setMode(mode);
            muteRepository.save(mute);
            return;
        }

        String groupId = request.groupId().trim();
        if (groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isEmpty()) {
            throw new IllegalArgumentException("グループのメンバーではありません");
        }
        if (mode.equals("NONE")) {
            muteRepository.deleteByUserIdAndGroupId(userId, groupId);
            return;
        }
        ConversationMute mute = muteRepository.findByUserIdAndGroupId(userId, groupId)
                .orElseGet(() -> new ConversationMute(userId, null, groupId, mode));
        mute.setMode(mode);
        muteRepository.save(mute);
    }

    @Transactional(readOnly = true)
    public boolean notificationsAllowed(Long receiverId, Long senderId, String groupId) {
        return notificationsAllowed(receiverId, senderId, groupId, false);
    }

    @Transactional(readOnly = true)
    public boolean notificationsAllowed(
            Long receiverId, Long senderId, String groupId, boolean isMentioned) {
        ConversationMute mute = groupId == null
                ? muteRepository.findByUserIdAndPartnerId(receiverId, senderId).orElse(null)
                : muteRepository.findByUserIdAndGroupId(receiverId, groupId).orElse(null);
        return mute == null || ("MENTIONS_ONLY".equals(mute.getMode()) && isMentioned);
    }
}
