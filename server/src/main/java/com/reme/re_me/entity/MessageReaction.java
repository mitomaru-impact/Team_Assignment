package com.reme.re_me.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "message_reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_message_reaction_user_emoji",
                        columnNames = {"message_id", "user_id", "emoji"}),
                @UniqueConstraint(
                        name = "uk_group_message_reaction_user_emoji",
                        columnNames = {"group_message_id", "user_id", "emoji"})
        })
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "group_message_id")
    private Long groupMessageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String emoji;

    public MessageReaction() {}

    public MessageReaction(Long messageId, Long groupMessageId, Long userId, String emoji) {
        this.messageId = messageId;
        this.groupMessageId = groupMessageId;
        this.userId = userId;
        this.emoji = emoji;
    }

    public Long getId() { return id; }
    public Long getMessageId() { return messageId; }
    public Long getGroupMessageId() { return groupMessageId; }
    public Long getUserId() { return userId; }
    public String getEmoji() { return emoji; }
}
