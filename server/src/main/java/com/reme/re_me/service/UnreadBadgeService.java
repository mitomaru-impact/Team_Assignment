package com.reme.re_me.service;

import com.reme.re_me.repository.GroupMessageRepository;
import com.reme.re_me.repository.MessageRepository;
import org.springframework.stereotype.Service;

@Service
public class UnreadBadgeService {

    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;

    public UnreadBadgeService(
            MessageRepository messageRepository,
            GroupMessageRepository groupMessageRepository) {
        this.messageRepository = messageRepository;
        this.groupMessageRepository = groupMessageRepository;
    }

    public int unreadCount(Long userId) {
        long unreadCount = messageRepository.countAllUnreadMessages(userId)
                + groupMessageRepository.countAllUnreadMessages(userId);
        return (int) Math.min(unreadCount, Integer.MAX_VALUE);
    }
}
