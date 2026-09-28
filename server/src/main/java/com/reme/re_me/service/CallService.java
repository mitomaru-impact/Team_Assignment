package com.reme.re_me.service;

import com.reme.re_me.dto.CallInvitationResponse;
import com.reme.re_me.dto.CallSignalRequest;
import com.reme.re_me.entity.CallInvitation;
import com.reme.re_me.entity.Message;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.CallInvitationRepository;
import com.reme.re_me.repository.MessageRepository;
import com.reme.re_me.repository.UserRepository;
import com.reme.re_me.service.ApnsPushNotificationService;
import com.reme.re_me.websocket.UserWebSocketEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CallService {

    private static final Duration INVITATION_LIFETIME = Duration.ofSeconds(45);
    private final CallInvitationRepository callRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatProfileService chatProfileService;
    private final ApnsPushNotificationService pushNotificationService;
    private final MessageRepository messageRepository;

    public CallService(
            CallInvitationRepository callRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            ChatProfileService chatProfileService,
            ApnsPushNotificationService pushNotificationService,
            MessageRepository messageRepository
    ) {
        this.callRepository = callRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.chatProfileService = chatProfileService;
        this.pushNotificationService = pushNotificationService;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public CallInvitationResponse startCall(User caller, String targetEmail) {
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "通話相手のメールアドレスが必要です");
        }
        User callee = userRepository.findByEmail(targetEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));
        if (caller.getId().equals(callee.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "自分自身には発信できません");
        }

        Instant now = Instant.now();
        callRepository.deleteByStatusInAndExpiresAtBefore(
                List.of("RINGING", "DECLINED", "ENDED"), now);
        String callId = UUID.randomUUID().toString();
        CallInvitation call = callRepository.save(new CallInvitation(
                callId, "p2p-" + callId, caller.getId(), callee.getId(), "RINGING",
                now, now.plus(INVITATION_LIFETIME)));

        String callerDisplayName = callParticipantNameAsSeenBy(callee, caller);
        sendAfterCommit(callee.getId(), Map.of(
                "type", "incoming_call",
                "callId", call.getId(),
                "callerEmail", caller.getEmail(),
                "callerName", callerDisplayName
        ));
        sendVoipPushAfterCommit(callee.getId(), call.getId(), callerDisplayName, caller.getEmail());
        return new CallInvitationResponse(call.getId(), call.getStatus().toLowerCase());
    }

    @Transactional
    public CallInvitationResponse accept(User callee, String callId) {
        CallInvitation call = requireCall(callId);
        if (!call.getCalleeUserId().equals(callee.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "この通話への応答は許可されていません");
        }
        if (!"RINGING".equals(call.getStatus()) || call.getExpiresAt().isBefore(Instant.now())) {
            call.setStatus("ENDED");
            throw new ResponseStatusException(HttpStatus.GONE, "この通話の呼び出しは終了しました");
        }

        call.setStatus("ACTIVE");
        call.setAcceptedAt(Instant.now());
        callRepository.save(call);
        User caller = userRepository.findById(call.getCallerUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "発信者が見つかりません"));
        String calleeVisibleName = callParticipantNameAsSeenBy(caller, callee);
        sendAfterCommit(caller.getId(), Map.of(
                "type", "call_accepted",
                "callId", call.getId(),
                "peerName", calleeVisibleName
        ));
        return new CallInvitationResponse(call.getId(), "active");
    }

    @Transactional
    public void decline(User callee, String callId) {
        CallInvitation call = requireCall(callId);
        requireParticipant(call, callee);
        if (!call.getCalleeUserId().equals(callee.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "この通話を拒否する権限がありません");
        }
        if ("RINGING".equals(call.getStatus())) {
            call.setStatus("DECLINED");
            callRepository.save(call);
            sendAfterCommit(call.getCallerUserId(), Map.of(
                    "type", "call_declined",
                    "callId", call.getId()
            ));
        }
    }

    @Transactional
    public void end(User user, String callId) {
        CallInvitation call = callRepository.findByIdForUpdate(callId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "通話が見つかりません"));
        requireParticipant(call, user);
        if ("ACTIVE".equals(call.getStatus())) {
            Instant endedAt = Instant.now();
            long durationSeconds = call.getAcceptedAt() == null
                    ? 0
                    : Math.max(0, Duration.between(call.getAcceptedAt(), endedAt).getSeconds());
            User caller = userRepository.findById(call.getCallerUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "発信者が見つかりません"));
            User callee = userRepository.findById(call.getCalleeUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "着信者が見つかりません"));
            Message callLog = messageRepository.save(Message.callLog(
                    caller.getId(), callee.getId(), call.getId(), durationSeconds));
            sendAfterCommit(caller.getId(), new com.reme.re_me.dto.MessageDto(
                    callLog, caller.getPublicId(), callee.getPublicId()));
            sendAfterCommit(callee.getId(), new com.reme.re_me.dto.MessageDto(
                    callLog, caller.getPublicId(), callee.getPublicId()));
            call.setStatus("ENDED");
            callRepository.save(call);
        } else if ("RINGING".equals(call.getStatus())) {
            call.setStatus("ENDED");
            callRepository.save(call);
        }
        Long otherUserId = call.getCallerUserId().equals(user.getId())
                ? call.getCalleeUserId() : call.getCallerUserId();
        sendAfterCommit(otherUserId, Map.of(
                "type", "call_ended",
                "callId", call.getId()
        ));
    }

    @Transactional
    public void relaySignal(Long senderUserId, CallSignalRequest signal) {
        if (signal.callId() == null || signal.callId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "通話IDが必要です");
        }
        CallInvitation call = requireCall(signal.callId());
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "アカウントが見つかりません"));
        requireParticipant(call, sender);
        if (!"ACTIVE".equals(call.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "通話はシグナリング可能な状態ではありません");
        }

        String type = signal.type();
        if ("call_offer".equals(type)) {
            if (!call.getCallerUserId().equals(sender.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "発信者以外は通話オファーを送信できません");
            }
            requireText(signal.sdp(), 262_144, "SDP");
        } else if ("call_answer".equals(type)) {
            if (!call.getCalleeUserId().equals(sender.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "着信側以外は通話応答を送信できません");
            }
            requireText(signal.sdp(), 262_144, "SDP");
        } else if ("ice_candidate".equals(type)) {
            requireText(signal.candidate(), 4_096, "ICE candidate");
            if (signal.sdpMLineIndex() == null || signal.sdpMLineIndex() < 0
                    || signal.sdpMLineIndex() > 64) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ICE candidate のメディア位置が不正です");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未対応の通話シグナリングです");
        }

        Long recipientUserId = call.getCallerUserId().equals(sender.getId())
                ? call.getCalleeUserId() : call.getCallerUserId();
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("callId", call.getId());
        if (signal.sdp() != null) event.put("sdp", signal.sdp());
        if (signal.candidate() != null) event.put("candidate", signal.candidate());
        if (signal.sdpMid() != null) event.put("sdpMid", signal.sdpMid());
        if (signal.sdpMLineIndex() != null) event.put("sdpMLineIndex", signal.sdpMLineIndex());
        sendAfterCommit(recipientUserId, event);
    }

    private void requireText(String value, int maxLength, String label) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " が空か、サイズ上限を超えています");
        }
    }

    private String callParticipantNameAsSeenBy(User viewer, User participant) {
        String profileName = chatProfileService.displayNameForEmailContact(viewer.getId(), participant.getEmail());
        if (profileName != null && !profileName.isBlank()
                && !profileName.equalsIgnoreCase(participant.getEmail())) {
            return profileName;
        }
        String accountName = participant.getName();
        return accountName == null || accountName.isBlank() ? participant.getEmail() : accountName;
    }

    private CallInvitation requireCall(String callId) {
        return callRepository.findById(callId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "通話が見つかりません"));
    }

    private void requireParticipant(CallInvitation call, User user) {
        if (!call.getCallerUserId().equals(user.getId()) && !call.getCalleeUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "この通話に参加する権限がありません");
        }
    }

    private void sendAfterCommit(Long userId, Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishEvent(new UserWebSocketEvent(userId, event));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new UserWebSocketEvent(userId, event));
            }
        });
    }

    private void sendVoipPushAfterCommit(Long userId, String callId, String callerName, String callerEmail) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pushNotificationService.sendIncomingCall(userId, callId, callerName, callerEmail);
            }
        });
    }
}
