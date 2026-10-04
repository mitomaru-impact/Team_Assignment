package com.reme.re_me.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "message_mentions",
        indexes = {
                @Index(name = "idx_message_mention_direct", columnList = "group_id, message_id"),
                @Index(name = "idx_message_mention_group", columnList = "mentioned_user_id")
        })
public class MessageMention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "group_id", length = 36)
    private String groupId;

    @Column(name = "mentioned_user_id", nullable = false)
    private Long mentionedUserId;

    @Column(name = "start_index", nullable = false)
    private int startIndex;

    @Column(nullable = false)
    private int length;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "everyone_mention", nullable = false)
    private boolean everyoneMention;

    protected MessageMention() {}

    public MessageMention(
            Long messageId,
            String groupId,
            Long mentionedUserId,
            int startIndex,
            int length,
            String displayName,
            boolean everyoneMention) {
        this.messageId = messageId;
        this.groupId = groupId;
        this.mentionedUserId = mentionedUserId;
        this.startIndex = startIndex;
        this.length = length;
        this.displayName = displayName;
        this.everyoneMention = everyoneMention;
    }

    public Long getMessageId() { return messageId; }
    public String getGroupId() { return groupId; }
    public Long getMentionedUserId() { return mentionedUserId; }
    public int getStartIndex() { return startIndex; }
    public int getLength() { return length; }
    public String getDisplayName() { return displayName; }
    public boolean isEveryoneMention() { return everyoneMention; }
}
