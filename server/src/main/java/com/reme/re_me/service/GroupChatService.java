package com.reme.re_me.service;

import com.reme.re_me.dto.CreateGroupRequest;
import com.reme.re_me.dto.GroupContactDto;
import com.reme.re_me.dto.GroupConversationDto;
import com.reme.re_me.dto.GroupMemberDto;
import com.reme.re_me.dto.MessageDto;
import com.reme.re_me.dto.SendGroupMessageRequest;
import com.reme.re_me.entity.ChatGroup;
import com.reme.re_me.entity.ChatGroupMember;
import com.reme.re_me.entity.GroupMessage;
import com.reme.re_me.entity.ProfileContact;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.ChatGroupMemberRepository;
import com.reme.re_me.repository.ChatGroupRepository;
import com.reme.re_me.repository.GroupMessageRepository;
import com.reme.re_me.repository.ProfileContactRepository;
import com.reme.re_me.repository.UserRepository;
import com.reme.re_me.websocket.MessageWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupChatService {

    private final ChatGroupRepository groupRepository;
    private final ChatGroupMemberRepository memberRepository;
    private final GroupMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProfileContactRepository profileContactRepository;
    private final ChatProfileService chatProfileService;
    private final PublicUserIdService publicUserIdService;
    private final MessageWebSocketHandler webSocketHandler;
    private final ApnsPushNotificationService pushNotificationService;

    public GroupChatService(
            ChatGroupRepository groupRepository,
            ChatGroupMemberRepository memberRepository,
            GroupMessageRepository messageRepository,
            UserRepository userRepository,
            ProfileContactRepository profileContactRepository,
            ChatProfileService chatProfileService,
            PublicUserIdService publicUserIdService,
            MessageWebSocketHandler webSocketHandler,
            ApnsPushNotificationService pushNotificationService) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.profileContactRepository = profileContactRepository;
        this.chatProfileService = chatProfileService;
        this.publicUserIdService = publicUserIdService;
        this.webSocketHandler = webSocketHandler;
        this.pushNotificationService = pushNotificationService;
    }

    @Transactional(readOnly = true)
    public List<GroupContactDto> getContacts(Long userId, Long profileId) {
        chatProfileService.findOwnedProfile(userId, profileId);
        return profileContactRepository.findAllByOwnerUserIdAndProfileId(userId, profileId).stream()
                .map(contact -> userRepository.findById(contact.getContactUserId())
                        .map(user -> new GroupContactDto(
                                user.getPublicId(),
                                user.getEmail(),
                                chatProfileService.displayNameForContact(userId, user.getId())))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupMemberDto> getMembers(Long userId, String groupId) {
        ChatGroup group = requireMember(userId, groupId);
        return memberRepository.findAllByGroupId(groupId).stream()
                .map(member -> userRepository.findById(member.getUserId())
                        .map(user -> new GroupMemberDto(
                                user.getPublicId(),
                                user.getEmail(),
                                user.getName(),
                                user.getId().equals(group.getCreatedBy())))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public List<GroupMemberDto> addMembers(Long userId, String groupId, List<String> publicIds) {
        ChatGroup group = requireCreator(userId, groupId);
        if (publicIds == null || publicIds.isEmpty()
                || new HashSet<>(publicIds).size() != publicIds.size()) {
            throw new IllegalArgumentException("追加するメンバーを選択してください");
        }
        List<ChatGroupMember> currentMembers = memberRepository.findAllByGroupId(groupId);
        Set<Long> existingIds = currentMembers.stream()
                .map(ChatGroupMember::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        if (currentMembers.size() + publicIds.size() > 100) {
            throw new IllegalArgumentException("グループのメンバーは100人までです");
        }
        List<Long> affectedIds = new ArrayList<>(existingIds);
        for (String publicId : publicIds) {
            User user = publicUserIdService.resolve(publicId);
            if (existingIds.contains(user.getId())) {
                throw new IllegalArgumentException("すでにグループに参加しているメンバーが含まれています");
            }
            memberRepository.save(new ChatGroupMember(groupId, user.getId()));
            affectedIds.add(user.getId());
        }
        afterCommit(() -> affectedIds.forEach(memberId -> webSocketHandler.sendCallEvent(
                memberId, java.util.Map.of("type", "group_members_changed", "groupId", groupId))));
        return getMembers(userId, groupId);
    }

    @Transactional
    public void removeMember(Long userId, String groupId, String publicUserId) {
        ChatGroup group = requireCreator(userId, groupId);
        User member = publicUserIdService.resolve(publicUserId);
        if (member.getId().equals(userId)) {
            throw new IllegalArgumentException("自分は管理画面から削除できません。グループから退出してください");
        }
        if (memberRepository.findByGroupIdAndUserId(groupId, member.getId()).isEmpty()) {
            throw new IllegalArgumentException("指定したユーザーはグループのメンバーではありません");
        }
        memberRepository.deleteByGroupIdAndUserId(groupId, member.getId());
        List<Long> remainingMemberIds = memberRepository.findAllByGroupId(groupId).stream()
                .map(ChatGroupMember::getUserId)
                .toList();
        afterCommit(() -> webSocketHandler.sendCallEvent(member.getId(), java.util.Map.of(
                "type", "group_member_removed",
                "groupId", groupId,
                "userId", member.getPublicId())));
        afterCommit(() -> remainingMemberIds.forEach(memberId -> webSocketHandler.sendCallEvent(
                memberId, java.util.Map.of("type", "group_members_changed", "groupId", groupId))));
    }

    @Transactional
    public void leaveGroup(Long userId, String groupId) {
        ChatGroup group = requireMember(userId, groupId);
        List<ChatGroupMember> members = memberRepository.findAllByGroupId(groupId);
        memberRepository.deleteByGroupIdAndUserId(groupId, userId);
        List<ChatGroupMember> remaining = members.stream()
                .filter(member -> !member.getUserId().equals(userId))
                .toList();
        if (remaining.isEmpty()) {
            messageRepository.deleteAllByGroupId(groupId);
            groupRepository.delete(group);
        } else {
            if (group.getCreatedBy().equals(userId)) {
                group.setCreatedBy(remaining.getFirst().getUserId());
                groupRepository.save(group);
            }
            User leavingUser = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));
            List<Long> remainingMemberIds = remaining.stream()
                    .map(ChatGroupMember::getUserId)
                    .toList();
            afterCommit(() -> webSocketHandler.sendCallEvent(leavingUser.getId(), java.util.Map.of(
                    "type", "group_member_removed",
                    "groupId", groupId,
                    "userId", leavingUser.getPublicId())));
            afterCommit(() -> remainingMemberIds.forEach(memberId -> webSocketHandler.sendCallEvent(
                    memberId, java.util.Map.of("type", "group_members_changed", "groupId", groupId))));
        }
    }

    @Transactional
    public void deleteGroup(Long userId, String groupId) {
        ChatGroup group = requireCreator(userId, groupId);
        List<Long> memberIds = memberRepository.findAllByGroupId(groupId).stream()
                .map(ChatGroupMember::getUserId)
                .toList();
        messageRepository.deleteAllByGroupId(groupId);
        memberRepository.deleteAllByGroupId(groupId);
        groupRepository.delete(group);
        afterCommit(() -> memberIds.forEach(memberId -> webSocketHandler.sendCallEvent(
                memberId, java.util.Map.of("type", "group_deleted", "groupId", groupId))));
    }

    @Transactional
    public GroupConversationDto createGroup(CreateGroupRequest request) {
        if (request.getUserId() == null || request.getProfileId() == null
                || request.getName() == null || request.getName().isBlank()
                || request.getName().trim().length() > 40) {
            throw new IllegalArgumentException("グループ名とプロファイルが必要です（グループ名は1〜40文字）");
        }
        User creator = publicUserIdService.resolve(request.getUserId());
        chatProfileService.findOwnedProfile(creator.getId(), request.getProfileId());
        List<String> selectedIds = request.getMemberPublicIds() == null
                ? List.of()
                : request.getMemberPublicIds();
        if (selectedIds.size() < 2 || selectedIds.size() > 99
                || new HashSet<>(selectedIds).size() != selectedIds.size()) {
            throw new IllegalArgumentException("グループには自分以外の2〜99人を選択してください");
        }

        Set<Long> allowedContactIds = profileContactRepository
                .findAllByOwnerUserIdAndProfileId(creator.getId(), request.getProfileId()).stream()
                .map(ProfileContact::getContactUserId)
                .collect(java.util.stream.Collectors.toSet());
        List<User> members = new ArrayList<>();
        members.add(creator);
        for (String publicId : selectedIds) {
            User contact = publicUserIdService.resolve(publicId);
            if (contact.getId().equals(creator.getId()) || !allowedContactIds.contains(contact.getId())) {
                throw new IllegalArgumentException("選択した相手はこのプロファイルの連絡先ではありません");
            }
            members.add(contact);
        }

        ChatGroup group = groupRepository.save(new ChatGroup(request.getName().trim(), creator.getId()));
        members.stream()
                .map(user -> new ChatGroupMember(group.getId(), user.getId()))
                .forEach(memberRepository::save);
        return new GroupConversationDto(group.getId(), group.getName(), null, 0);
    }

    @Transactional(readOnly = true)
    public List<GroupConversationDto> getGroups(Long userId) {
        List<GroupConversationDto> conversations = new ArrayList<>();
        for (ChatGroupMember member : memberRepository.findAllByUserId(userId)) {
            ChatGroup group = groupRepository.findById(member.getGroupId())
                    .orElseThrow(() -> new IllegalStateException("グループ情報が見つかりません"));
            GroupMessage latest = messageRepository
                    .findAllByGroupIdOrderByCreatedAtDescIdDesc(group.getId()).stream()
                    .findFirst().orElse(null);
            long unread = member.getLastReadAt() == null
                    ? messageRepository.countUnread(group.getId(), userId)
                    : messageRepository.countUnreadAfter(group.getId(), userId, member.getLastReadAt());
            conversations.add(new GroupConversationDto(group.getId(), group.getName(), latest, unread));
        }
        conversations.sort(Comparator.comparing(
                GroupConversationDto::getLastMessageTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return conversations;
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getHistory(Long userId, String groupId) {
        ChatGroup group = requireMember(userId, groupId);
        List<ChatGroupMember> members = memberRepository.findAllByGroupId(groupId);
        return messageRepository.findAllByGroupIdOrderByCreatedAtAscIdAsc(groupId).stream()
                .map(message -> toDto(message, group, members))
                .toList();
    }

    public MessageDto sendMessage(String groupId, SendGroupMessageRequest request) {
        if (request.getUserId() == null || request.getContent() == null
                || request.getContent().isBlank() || request.getContent().trim().length() > 1000) {
            throw new IllegalArgumentException("メッセージは1〜1000文字で入力してください");
        }
        User sender = publicUserIdService.resolve(request.getUserId());
        ChatGroup group = requireMember(sender.getId(), groupId);
        GroupMessage replyTo = null;
        if (request.getReplyToMessageId() != null) {
            replyTo = messageRepository.findById(request.getReplyToMessageId())
                    .filter(message -> message.getGroupId().equals(groupId))
                    .orElseThrow(() -> new IllegalArgumentException("返信先のメッセージが見つかりません"));
        }
        GroupMessage saved = messageRepository.save(new GroupMessage(
                groupId, sender.getId(), sender.getName(), request.getContent().trim(), replyTo));
        List<ChatGroupMember> members = memberRepository.findAllByGroupId(groupId);
        MessageDto dto = toDto(saved, group, members);
        for (ChatGroupMember member : members) {
            webSocketHandler.sendToUser(member.getUserId(), dto);
            if (!member.getUserId().equals(sender.getId())) {
                pushNotificationService.sendNewGroupMessage(
                        member.getUserId(),
                        saved.getId(),
                        saved.getContent(),
                        groupId,
                        group.getName(),
                        sender.getName());
            }
        }
        return dto;
    }

    @Transactional
    public MessageDto editMessage(Long userId, String groupId, Long messageId, String content) {
        if (content == null || content.isBlank() || content.trim().length() > 1000) {
            throw new IllegalArgumentException("メッセージは1〜1000文字で入力してください");
        }
        ChatGroup group = requireMember(userId, groupId);
        GroupMessage message = messageRepository.findById(messageId)
                .filter(candidate -> candidate.getGroupId().equals(groupId))
                .orElseThrow(() -> new IllegalArgumentException("メッセージが見つかりません"));
        if (!message.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("自分のメッセージだけ編集できます");
        }
        message.setContent(content.trim());
        GroupMessage saved = messageRepository.save(message);
        List<ChatGroupMember> members = memberRepository.findAllByGroupId(groupId);
        MessageDto dto = toDto(saved, group, members);
        List<Long> memberIds = members.stream()
                .map(ChatGroupMember::getUserId)
                .toList();
        afterCommit(() -> memberIds.forEach(memberId -> webSocketHandler.sendToUser(memberId, dto)));
        return dto;
    }

    @Transactional
    public void markAsRead(Long userId, String groupId) {
        ChatGroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("グループのメンバーではありません"));
        member.setLastReadAt(LocalDateTime.now());
        memberRepository.save(member);
        List<Long> memberIds = memberRepository.findAllByGroupId(groupId).stream()
                .map(ChatGroupMember::getUserId)
                .toList();
        afterCommit(() -> memberIds.forEach(memberId -> webSocketHandler.sendCallEvent(
                memberId, java.util.Map.of("type", "group_read_receipt", "groupId", groupId))));
    }

    private ChatGroup requireMember(Long userId, String groupId) {
        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("グループが見つかりません"));
        if (memberRepository.findByGroupIdAndUserId(groupId, userId).isEmpty()) {
            throw new IllegalArgumentException("グループのメンバーではありません");
        }
        return group;
    }

    private ChatGroup requireCreator(Long userId, String groupId) {
        ChatGroup group = requireMember(userId, groupId);
        if (!group.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("グループ作成者のみ実行できます");
        }
        return group;
    }

    private MessageDto toDto(GroupMessage message, ChatGroup group, List<ChatGroupMember> members) {
        User sender = userRepository.findById(message.getSenderId())
                .orElseThrow(() -> new IllegalStateException("送信者が見つかりません"));
        MessageDto dto = new MessageDto(message, sender.getPublicId(), sender.getEmail(), group.getName());
        int readCount = (int) members.stream()
                .filter(member -> !member.getUserId().equals(message.getSenderId()))
                .filter(member -> member.getLastReadAt() != null
                        && !member.getLastReadAt().isBefore(message.getCreatedAt()))
                .count();
        dto.setReadCount(readCount);
        return dto;
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
