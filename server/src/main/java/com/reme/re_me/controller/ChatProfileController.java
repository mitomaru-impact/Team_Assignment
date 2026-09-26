package com.reme.re_me.controller;

import com.reme.re_me.dto.AddProfileContactRequest;
import com.reme.re_me.dto.ChatProfileDto;
import com.reme.re_me.dto.CreateChatProfileRequest;
import com.reme.re_me.service.ChatProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
public class ChatProfileController {

    private final ChatProfileService profileService;

    public ChatProfileController(ChatProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<?> getProfiles(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(profileService.getProfiles(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createProfile(@RequestBody CreateChatProfileRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("プロフィール情報が必要です");
        }
        try {
            return ResponseEntity.ok(profileService.createProfile(request.getUserId(), request.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long profileId, @RequestParam Long userId) {
        try {
            profileService.deleteProfile(userId, profileId);
            return ResponseEntity.noContent().build();
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
            profileService.addContact(request.getUserId(), profileId, request.getEmail());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{profileId}/contacts")
    public ResponseEntity<?> removeContact(
            @PathVariable Long profileId,
            @RequestParam Long userId,
            @RequestParam String email) {
        try {
            profileService.removeContact(userId, profileId, email);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{profileId}/contacts/{contactUserId}")
    public ResponseEntity<?> assignContact(
            @PathVariable Long profileId,
            @PathVariable Long contactUserId,
            @RequestParam Long userId) {
        try {
            profileService.assignUnclassifiedContact(userId, profileId, contactUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
