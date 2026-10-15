package com.reme.re_me.service;

import com.reme.re_me.dto.BlockedUserDto;
import com.reme.re_me.dto.UserBlockRequest;
import com.reme.re_me.dto.UpdateBlockedUserProfileRequest;
import com.reme.re_me.entity.ChatProfile;
import com.reme.re_me.entity.User;
import com.reme.re_me.entity.UserBlock;
import com.reme.re_me.repository.UserBlockRepository;
import com.reme.re_me.repository.UserRepository;
import com.reme.re_me.repository.ChatProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserBlockService {

    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;
    private final PublicUserIdService publicUserIdService;
    private final ChatProfileRepository chatProfileRepository;

    public UserBlockService(
            UserBlockRepository blockRepository,
            UserRepository userRepository,
            PublicUserIdService publicUserIdService,
            ChatProfileRepository chatProfileRepository) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.publicUserIdService = publicUserIdService;
        this.chatProfileRepository = chatProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<BlockedUserDto> list(String publicUserId) {
        Long userId = publicUserIdService.resolveInternalId(publicUserId);
        return blockRepository.findAllByBlockerId(userId).stream()
                .map(block -> userRepository.findById(block.getBlockedId())
                        .map(user -> {
                            ChatProfile profile = block.getVisibleProfileId() == null
                                    ? null
                                    : chatProfileRepository.findByIdAndUserId(
                                            block.getVisibleProfileId(), userId).orElse(null);
                            return new BlockedUserDto(
                                    user.getEmail(),
                                    user.getPublicId(),
                                    block.getVisibleProfileId(),
                                    profile == null ? null : profile.getName());
                        })
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public void block(UserBlockRequest request) {
        if (request == null || request.userId() == null
                || request.targetEmail() == null || request.targetEmail().isBlank()) {
            throw new IllegalArgumentException("ブロック対象のユーザーが必要です");
        }
        Long blockerId = publicUserIdService.resolveInternalId(request.userId());
        User blocked = userRepository.findByEmail(request.targetEmail().trim())
                .orElseThrow(() -> new IllegalArgumentException("指定されたユーザーが見つかりません"));
        if (blockerId.equals(blocked.getId())) {
            throw new IllegalArgumentException("自分自身はブロックできません");
        }
        blockRepository.findByBlockerIdAndBlockedId(blockerId, blocked.getId())
                .orElseGet(() -> blockRepository.save(new UserBlock(blockerId, blocked.getId())));
    }

    @Transactional
    public void unblock(String publicUserId, String targetEmail) {
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalArgumentException("ブロック解除対象のユーザーが必要です");
        }
        Long blockerId = publicUserIdService.resolveInternalId(publicUserId);
        User blocked = userRepository.findByEmail(targetEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException("指定されたユーザーが見つかりません"));
        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blocked.getId());
    }

    @Transactional
    public void updateVisibleProfile(UpdateBlockedUserProfileRequest request) {
        if (request == null || request.userId() == null
                || request.targetEmail() == null || request.targetEmail().isBlank()
                || request.profileId() == null) {
            throw new IllegalArgumentException("対象ユーザーとプロファイルが必要です");
        }
        Long blockerId = publicUserIdService.resolveInternalId(request.userId());
        chatProfileRepository.findByIdAndUserId(request.profileId(), blockerId)
                .orElseThrow(() -> new IllegalArgumentException("指定されたプロファイルが見つかりません"));
        User blocked = userRepository.findByEmail(request.targetEmail().trim())
                .orElseThrow(() -> new IllegalArgumentException("指定されたユーザーが見つかりません"));
        UserBlock block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blocked.getId())
                .orElseThrow(() -> new IllegalArgumentException("ブロック情報が見つかりません"));
        block.setVisibleProfileId(request.profileId());
        blockRepository.save(block);
    }

    @Transactional(readOnly = true)
    public boolean blocks(Long blockerId, Long blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }
}
