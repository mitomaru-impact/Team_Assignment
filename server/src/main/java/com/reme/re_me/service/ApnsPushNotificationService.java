package com.reme.re_me.service;

import com.reme.re_me.entity.ApnsDeviceToken;
import com.reme.re_me.repository.ApnsDeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ApnsPushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ApnsPushNotificationService.class);
    private static final long JWT_REFRESH_SECONDS = 50 * 60;

    private final ApnsDeviceTokenRepository tokenRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String teamId;
    private final String keyId;
    private final String bundleId;
    private final String privateKeyPath;
    private final boolean useSandbox;
    private volatile PrivateKey privateKey;
    private volatile String cachedJwt;
    private volatile long cachedJwtCreatedAt;

    public ApnsPushNotificationService(
            ApnsDeviceTokenRepository tokenRepository,
            ObjectMapper objectMapper,
            @Value("${apns.team-id:}") String teamId,
            @Value("${apns.key-id:}") String keyId,
            @Value("${apns.bundle-id:com.reme.re-me}") String bundleId,
            @Value("${apns.private-key-path:}") String privateKeyPath,
            @Value("${apns.use-sandbox:true}") boolean useSandbox) {
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
        this.teamId = teamId;
        this.keyId = keyId;
        this.bundleId = bundleId;
        this.privateKeyPath = privateKeyPath;
        this.useSandbox = useSandbox;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        if (!isConfigured()) {
            logger.info("APNs is not configured; push notifications are disabled");
        } else {
            logger.info("APNs push notifications are enabled (sandbox: {})", useSandbox);
        }
    }

    public void sendNewMessage(Long receiverId, Long messageId, String messageContent, String senderEmail) {
        if (!isConfigured()) {
            logger.error("Cannot send APNs notification for message {}: APNs configuration is incomplete", messageId);
            return;
        }

        List<ApnsDeviceToken> tokens = tokenRepository.findAllByUserId(receiverId);
        if (tokens.isEmpty()) {
            logger.warn("No APNs device token registered for message recipient {}", receiverId);
            return;
        }
        logger.info("Sending APNs notification for message {} to {} device(s) registered for user {}",
                messageId, tokens.size(), receiverId);
        for (ApnsDeviceToken token : tokens) {
            sendToDevice(token, messageId, messageContent, senderEmail);
        }
    }

    private void sendToDevice(ApnsDeviceToken device, Long messageId, String messageContent, String senderEmail) {
        try {
            Map<String, Object> alert = Map.of(
                    "title", "Re:Me",
                    "body", messageContent);
            Map<String, Object> payload = Map.of(
                    "aps", Map.of("alert", alert, "sound", "default"),
                    "messageId", messageId,
                    "senderEmail", senderEmail);
            String host = useSandbox
                    ? "https://api.sandbox.push.apple.com"
                    : "https://api.push.apple.com";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(host + "/3/device/" + device.getToken()))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "bearer " + createJwt())
                    .header("apns-topic", bundleId)
                    .header("apns-push-type", "alert")
                    .header("apns-priority", "10")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("APNs accepted notification for message {}", messageId);
                return;
            }

            if (response.statusCode() == 410 || response.body().contains("BadDeviceToken")) {
                tokenRepository.deleteByToken(device.getToken());
            }
            logger.warn("APNs rejected notification for device token (status {}): {}",
                    response.statusCode(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while sending an APNs notification", e);
        } catch (Exception e) {
            logger.error("Failed to send an APNs notification", e);
        }
    }

    private boolean isConfigured() {
        return !teamId.isBlank() && !keyId.isBlank() && !bundleId.isBlank()
                && !privateKeyPath.isBlank();
    }

    private synchronized String createJwt() throws Exception {
        long now = Instant.now().getEpochSecond();
        if (cachedJwt != null && now - cachedJwtCreatedAt < JWT_REFRESH_SECONDS) {
            return cachedJwt;
        }

        String header = base64Url(objectMapper.writeValueAsBytes(Map.of("alg", "ES256", "kid", keyId)));
        String claims = base64Url(objectMapper.writeValueAsBytes(Map.of("iss", teamId, "iat", now)));
        String signingInput = header + "." + claims;
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(getPrivateKey());
        signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        cachedJwt = signingInput + "." + base64Url(derToJose(signer.sign()));
        cachedJwtCreatedAt = now;
        return cachedJwt;
    }

    private PrivateKey getPrivateKey() throws Exception {
        if (privateKey == null) {
            synchronized (this) {
                if (privateKey == null) {
                    String pem = Files.readString(Path.of(privateKeyPath));
                    String encodedKey = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                            .replace("-----END PRIVATE KEY-----", "")
                            .replaceAll("\\s", "");
                    byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
                    privateKey = KeyFactory.getInstance("EC")
                            .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
                }
            }
        }
        return privateKey;
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] derToJose(byte[] der) throws IOException {
        if (der.length < 8 || der[0] != 0x30 || der[2] != 0x02) {
            throw new IOException("Invalid ECDSA signature encoding");
        }
        int rLength = der[3] & 0xff;
        int sMarker = 4 + rLength;
        if (sMarker + 2 > der.length || der[sMarker] != 0x02) {
            throw new IOException("Invalid ECDSA signature encoding");
        }
        int sLength = der[sMarker + 1] & 0xff;
        byte[] jose = new byte[64];
        copyInteger(der, 4, rLength, jose, 0);
        copyInteger(der, sMarker + 2, sLength, jose, 32);
        return jose;
    }

    private static void copyInteger(byte[] source, int offset, int length, byte[] target, int targetOffset)
            throws IOException {
        while (length > 32 && source[offset] == 0) {
            offset++;
            length--;
        }
        if (length > 32) {
            throw new IOException("ECDSA signature component is too large");
        }
        System.arraycopy(source, offset, target, targetOffset + 32 - length, length);
    }
}
