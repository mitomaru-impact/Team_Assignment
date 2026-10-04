package com.reme.re_me.service;

import com.reme.re_me.dto.MessageMentionDto;
import com.reme.re_me.entity.MessageMention;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.MessageMentionRepository;
import com.reme.re_me.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageMentionService {

    private final MessageMentionRepository mentionRepository;
    private final UserRepository userRepository;
    private final ChatProfileService chatProfileService;

    public MessageMentionService(
            MessageMentionRepository mentionRepository,
            UserRepository userRepository,
            ChatProfileService chatProfileService) {
        this.mentionRepository = mentionRepository;
        this.userRepository = userRepository;
        this.chatProfileService = chatProfileService;
    }

    @Transactional
    public Resolution resolve(String content, Long senderId, List<User> eligibleUsers, String groupId) {
        Map<Long, User> eligible = new LinkedHashMap<>();
        eligibleUsers.stream()
                .filter(user -> !user.getId().equals(senderId))
                .forEach(user -> eligible.put(user.getId(), user));

        StringBuilder normalized = new StringBuilder();
        List<ResolvedMention> resolved = new ArrayList<>();
        int sourceOffset = 0;
        while (sourceOffset < content.length()) {
            int atIndex = content.indexOf('@', sourceOffset);
            if (atIndex < 0) {
                break;
            }
            normalized.append(content, sourceOffset, atIndex);
            int tokenStart = atIndex + 1;
            if (atIndex > 0 && !isMentionBoundary(content.charAt(atIndex - 1))) {
                normalized.append('@');
                sourceOffset = tokenStart;
                continue;
            }
            if (content.regionMatches(true, tokenStart, "everyone", 0, "everyone".length())
                    && hasTokenBoundary(content, tokenStart + "everyone".length())) {
                if (groupId == null) {
                    throw new IllegalArgumentException("@everyone はグループでのみ使用できます");
                }
                int startIndex = normalized.length();
                String displayToken = "@everyone";
                normalized.append(displayToken);
                for (User target : eligible.values()) {
                    resolved.add(new ResolvedMention(
                            target, startIndex, displayToken.length(), "everyone", true));
                }
                sourceOffset = tokenStart + "everyone".length();
            } else {
                AliasMatch match = findAlias(content, tokenStart, eligible, senderId);
                if (match == null) {
                    normalized.append('@');
                    sourceOffset = tokenStart;
                    continue;
                }
                if (match.users().size() > 1) {
                    throw new IllegalArgumentException("同じニックネームの相手が複数います。メールアドレスで指定してください");
                }
                User target = match.users().getFirst();
                String nickname = nicknameFor(senderId, target);
                String rendered = "@" + nickname;
                int startIndex = normalized.length();
                normalized.append(rendered);
                resolved.add(new ResolvedMention(
                        target, startIndex, rendered.length(), nickname, false));
                sourceOffset = tokenStart + match.aliasLength();
            }
        }
        normalized.append(content.substring(sourceOffset));
        return new Resolution(normalized.toString(), resolved);
    }

    private AliasMatch findAlias(String content, int start, Map<Long, User> eligible, Long senderId) {
        int longestLength = 0;
        Map<Long, User> matchedUsers = new LinkedHashMap<>();
        for (User user : eligible.values()) {
            List<String> aliases = List.of(
                    user.getName(),
                    chatProfileService.displayNameForEmailContact(senderId, user.getEmail()));
            for (String alias : aliases) {
                if (alias == null || alias.isBlank()
                        || alias.length() < longestLength
                        || !content.regionMatches(true, start, alias, 0, alias.length())
                        || !hasTokenBoundary(content, start + alias.length())) {
                    continue;
                }
                if (alias.length() > longestLength) {
                    longestLength = alias.length();
                    matchedUsers.clear();
                }
                matchedUsers.put(user.getId(), user);
            }
        }
        return longestLength == 0 ? null : new AliasMatch(longestLength, List.copyOf(matchedUsers.values()));
    }

    private boolean hasTokenBoundary(String content, int index) {
        if (index >= content.length()) {
            return true;
        }
        char next = content.charAt(index);
        return Character.isWhitespace(next) || ",.!?:;)]}，。、！？；：".indexOf(next) >= 0;
    }

    private boolean isMentionBoundary(char previous) {
        return Character.isWhitespace(previous) || "([{<,.;!?，。、！？".indexOf(previous) >= 0;
    }

    private String nicknameFor(Long senderId, User user) {
        String displayName = chatProfileService.displayNameForEmailContact(senderId, user.getEmail());
        if (displayName == null || displayName.isBlank() || displayName.equals(user.getEmail())) {
            return user.getName();
        }
        return displayName;
    }

    @Transactional
    public void save(Long messageId, String groupId, Resolution resolution) {
        for (ResolvedMention mention : resolution.mentions()) {
            mentionRepository.save(new MessageMention(
                    messageId,
                    groupId,
                    mention.user().getId(),
                    mention.startIndex(),
                    mention.length(),
                    mention.displayName(),
                    mention.everyone()));
        }
    }

    @Transactional(readOnly = true)
    public List<MessageMentionDto> forMessage(Long messageId, String groupId, Long viewerId) {
        List<MessageMention> mentions = groupId == null
                ? mentionRepository.findAllByGroupIdIsNullAndMessageId(messageId)
                : mentionRepository.findAllByGroupIdAndMessageId(groupId, messageId);
        return toDtos(mentions, viewerId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<MessageMentionDto>> forMessages(
            List<Long> messageIds, String groupId, Long viewerId) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        List<MessageMention> mentions = groupId == null
                ? mentionRepository.findAllByGroupIdIsNullAndMessageIdIn(messageIds)
                : mentionRepository.findAllByGroupIdAndMessageIdIn(groupId, messageIds);
        Map<Long, List<MessageMentionDto>> result = new LinkedHashMap<>();
        for (MessageMention mention : mentions) {
            result.computeIfAbsent(mention.getMessageId(), ignored -> new ArrayList<>())
                    .add(toDto(mention, viewerId));
        }
        return result;
    }

    public boolean mentionsUser(Long messageId, String groupId, Long userId) {
        return (groupId == null
                ? mentionRepository.findAllByGroupIdIsNullAndMessageId(messageId)
                : mentionRepository.findAllByGroupIdAndMessageId(groupId, messageId))
                .stream().anyMatch(mention -> mention.getMentionedUserId().equals(userId));
    }

    @Transactional
    public void deleteForMessage(Long messageId, String groupId) {
        if (groupId == null) {
            mentionRepository.deleteAllByGroupIdIsNullAndMessageId(messageId);
        } else {
            mentionRepository.deleteAllByGroupIdAndMessageId(groupId, messageId);
        }
    }

    @Transactional
    public void deleteForGroup(String groupId) {
        mentionRepository.deleteAllByGroupId(groupId);
    }

    private List<MessageMentionDto> toDtos(List<MessageMention> mentions, Long viewerId) {
        return mentions.stream().map(mention -> toDto(mention, viewerId)).toList();
    }

    private MessageMentionDto toDto(MessageMention mention, Long viewerId) {
        String publicId = userRepository.findById(mention.getMentionedUserId())
                .map(User::getPublicId)
                .orElse("");
        return new MessageMentionDto(
                mention.getStartIndex(),
                mention.getLength(),
                publicId,
                mention.getDisplayName(),
                mention.getMentionedUserId().equals(viewerId),
                mention.isEveryoneMention());
    }

    public record Resolution(String content, List<ResolvedMention> mentions) {}
    public record ResolvedMention(
            User user, int startIndex, int length, String displayName, boolean everyone) {}
    private record AliasMatch(int aliasLength, List<User> users) {}

}
