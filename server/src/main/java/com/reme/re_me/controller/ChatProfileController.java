package com.reme.re_me.controller;

import com.reme.re_me.dto.AddProfileContactRequest;
import com.reme.re_me.dto.ChatProfileDto;
import com.reme.re_me.dto.CreateChatProfileRequest;
import com.reme.re_me.dto.UpdateChatProfileIdentityRequest;
import com.reme.re_me.dto.UserDisplayNameDto;
import com.reme.re_me.service.ChatProfileService;
import com.reme.re_me.service.PublicUserIdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
public class ChatProfileController {

    private final ChatProfileService profileService;
    private final PublicUserIdService publicUserIdService;

    public ChatProfileController(ChatProfileService profileService, PublicUserIdService publicUserIdService) {
        this.profileService = profileService;
        this.publicUserIdService = publicUserIdService;
    }

    @GetMapping
    public ResponseEntity<?> getProfiles(@RequestParam String userId) {
        try {
            return ResponseEntity.ok(profileService.getProfiles(publicUserIdService.resolveInternalId(userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createProfile(@RequestBody CreateChatProfileRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("プロファイル情報が必要です");
        }
        try {
            return ResponseEntity.ok(profileService.createProfile(
                    publicUserIdService.resolveInternalId(request.getUserId()), request.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long profileId, @RequestParam String userId) {
        try {
            profileService.deleteProfile(publicUserIdService.resolveInternalId(userId), profileId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{profileId}/identity")
    public ResponseEntity<?> updateIdentity(
            @PathVariable Long profileId,
            @RequestBody UpdateChatProfileIdentityRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("プロファイル情報が必要です");
        }
        try {
            return ResponseEntity.ok(profileService.updateDisplayName(
                    publicUserIdService.resolveInternalId(request.getUserId()),
                    profileId,
                    request.getDisplayName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/contacts/display-name")
    public ResponseEntity<?> getContactDisplayName(
            @RequestParam String userId,
            @RequestParam String contactEmail) {
        try {
            Long ownerId = publicUserIdService.resolveInternalId(userId);
            return ResponseEntity.ok(new UserDisplayNameDto(
                    profileService.displayNameForEmailContact(ownerId, contactEmail)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{profileId}/contacts")
    public ResponseEntity<?> addContact(
            @PathVariable Long profileId,
            @RequestBody AddProfileContactRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("連絡先情報が必要です");
        }
        try {
            profileService.addContact(
                    publicUserIdService.resolveInternalId(request.getUserId()), profileId, request.getEmail());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{profileId}/contacts/by-user/{publicUserId}")
    public ResponseEntity<?> addContactByPublicId(
            @PathVariable Long profileId,
            @RequestParam String userId,
            @PathVariable String publicUserId) {
        try {
            profileService.addContactById(
                    publicUserIdService.resolveInternalId(userId),
                    profileId,
                    publicUserIdService.resolveInternalId(publicUserId));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{profileId}/contacts")
    public ResponseEntity<?> removeContact(
            @PathVariable Long profileId,
            @RequestParam String userId,
            @RequestParam String email) {
        try {
            profileService.removeContact(publicUserIdService.resolveInternalId(userId), profileId, email);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{profileId}/contacts/{contactUserId}")
    public ResponseEntity<?> assignContact(
            @PathVariable Long profileId,
            @PathVariable String contactUserId,
            @RequestParam String userId) {
        try {
            profileService.assignUnclassifiedContact(
                    publicUserIdService.resolveInternalId(userId),
                    profileId,
                    publicUserIdService.resolveInternalId(contactUserId));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
