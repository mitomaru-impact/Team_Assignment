package com.reme.re_me.controller;

import com.reme.re_me.dto.CreateGroupRequest;
import com.reme.re_me.dto.ManageGroupMembersRequest;
import com.reme.re_me.dto.SendGroupMessageRequest;
import com.reme.re_me.service.GroupChatService;
import com.reme.re_me.service.PublicUserIdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class GroupChatController {

    private final GroupChatService groupChatService;
    private final PublicUserIdService publicUserIdService;

    public GroupChatController(GroupChatService groupChatService, PublicUserIdService publicUserIdService) {
        this.groupChatService = groupChatService;
        this.publicUserIdService = publicUserIdService;
    }

    @GetMapping("/contacts")
    public ResponseEntity<?> getContacts(
            @RequestParam String userId,
            @RequestParam Long profileId) {
        try {
            return ResponseEntity.ok(groupChatService.getContacts(
                    publicUserIdService.resolveInternalId(userId), profileId));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("グループ情報が必要です");
        }
        try {
            return ResponseEntity.ok(groupChatService.createGroup(request));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getGroups(@RequestParam String userId) {
        try {
            return ResponseEntity.ok(groupChatService.getGroups(
                    publicUserIdService.resolveInternalId(userId)));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String groupId, @RequestParam String userId) {
        try {
            return ResponseEntity.ok(groupChatService.getHistory(
                    publicUserIdService.resolveInternalId(userId), groupId));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PostMapping("/{groupId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable String groupId,
            @RequestBody SendGroupMessageRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("メッセージの送信内容が必要です");
        }
        try {
            return ResponseEntity.ok(groupChatService.sendMessage(groupId, request));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PutMapping("/{groupId}/messages/{messageId}")
    public ResponseEntity<?> editMessage(
            @PathVariable String groupId,
            @PathVariable Long messageId,
            @RequestBody com.reme.re_me.dto.EditMessageRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("編集内容が必要です");
        }
        try {
            return ResponseEntity.ok(groupChatService.editMessage(
                    publicUserIdService.resolveInternalId(request.getUserId()),
                    groupId,
                    messageId,
                    request.getContent()));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PutMapping("/{groupId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String groupId, @RequestParam String userId) {
        try {
            groupChatService.markAsRead(publicUserIdService.resolveInternalId(userId), groupId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getMembers(@PathVariable String groupId, @RequestParam String userId) {
        try {
            return ResponseEntity.ok(groupChatService.getMembers(
                    publicUserIdService.resolveInternalId(userId), groupId));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMembers(
            @PathVariable String groupId,
            @RequestBody ManageGroupMembersRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("追加するメンバーが必要です");
        }
        try {
            return ResponseEntity.ok(groupChatService.addMembers(
                    publicUserIdService.resolveInternalId(request.getUserId()),
                    groupId,
                    request.getMemberPublicIds()));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @DeleteMapping("/{groupId}/members/{memberPublicId}")
    public ResponseEntity<?> removeMember(
            @PathVariable String groupId,
            @PathVariable String memberPublicId,
            @RequestParam String userId) {
        try {
            groupChatService.removeMember(
                    publicUserIdService.resolveInternalId(userId), groupId, memberPublicId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<?> leaveGroup(@PathVariable String groupId, @RequestParam String userId) {
        try {
            groupChatService.leaveGroup(publicUserIdService.resolveInternalId(userId), groupId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable String groupId, @RequestParam String userId) {
        try {
            groupChatService.deleteGroup(publicUserIdService.resolveInternalId(userId), groupId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }
}
