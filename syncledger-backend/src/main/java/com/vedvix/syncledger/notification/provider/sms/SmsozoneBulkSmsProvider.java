package com.vedvix.syncledger.notification.provider.sms;

import com.vedvix.syncledger.notification.exception.ProviderException;
import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.domain.NotificationResult;
import com.vedvix.syncledger.notification.domain.Recipient;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

/**
 * Alternative implementation for SMSOzone using POST method
 * Enable this if the GET method doesn't work or if you need to send POST requests
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.providers.sms.smsozone", name = "use-post-method", havingValue = "true")
public class SmsozoneBulkSmsProvider implements SmsProvider {

    private final SmsozoneConfig config;
    private final RestTemplate restTemplate;

    @Override
    public NotificationResult send(NotificationRequest request) {
        log.info("Sending SMS notification {} using SMSOzone (POST method)", request.id());

        try {
            for (Recipient recipient : request.recipients()) {
                if (recipient.phoneNumber() == null || recipient.phoneNumber().isBlank()) {
                    log.warn("Skipping recipient without phone number for notification {}", request.id());
                    continue;
                }

                String message = buildMessage(request);
                String messageId = sendSmsPost(recipient.phoneNumber(), message);

                log.info("SMS sent successfully via SMSOzone (POST). Message ID: {}", messageId);

                return NotificationResult.builder()
                        .notificationId(request.id())
                        .recipientId(recipient.phoneNumber())
                        .status(NotificationStatus.SENT)
                        .providerId(getProviderId())
                        .externalId(messageId)
                        .sentAt(Instant.now())
                        .attemptCount(1)
                        .build();
            }

            throw new ProviderException("NO_RECIPIENTS", "No valid recipients found", false);

        } catch (Exception e) {
            log.error("Failed to send SMS via SMSOzone (POST): {}", e.getMessage(), e);
            throw new ProviderException(
                    "SMSOZONE_ERROR",
                    "Failed to send SMS: " + e.getMessage(),
                    e,
                    true
            );
        }
    }

    private String sendSmsPost(String phoneNumber, String message) {
        String cleanedNumber = cleanPhoneNumber(phoneNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", config.getApiKey());
        params.add("sender", config.getSenderId());
        params.add("channel", "Trans");
        params.add("DCS", "0");
        params.add("flashsms", "0");
        params.add("number", cleanedNumber);
        params.add("text", message);
        params.add("route", "2069");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                config.getApiUrl(),
                HttpMethod.POST,
                entity,
                String.class
        );

        String responseBody = response.getBody();
        log.debug("SMSOzone POST response: {}", responseBody);

        if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
            return parseMessageId(responseBody, cleanedNumber);
        } else {
            throw new ProviderException(
                    "SMSOZONE_INVALID_RESPONSE",
                    "Invalid response from SMSOzone: " + responseBody,
                    false
            );
        }
    }

    private String buildMessage(NotificationRequest request) {
        Object message = request.parameters().get("message");
        if (message != null) {
            return message.toString();
        }

        Object otp = request.parameters().get("otp");
        if (otp != null) {
            return String.format(
                    "KAPS Your OTP is %s. Please use this code to complete your verification. Do not share this OTP with anyone.",
                    otp
            );
        }

        String template = (String) request.parameters().getOrDefault(
                "template",
                "KAPS: Hello {{firstName}}, {{message}}"
        );

        for (var entry : request.parameters().entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}",
                    String.valueOf(entry.getValue()));
        }

        return template;
    }

    private String cleanPhoneNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");

        if (cleaned.startsWith("+91")) {
            return cleaned;
        }

        if (cleaned.startsWith("91") && cleaned.length() == 12) {
            return "+" + cleaned;
        }

        if (cleaned.length() == 10) {
            return "+91" + cleaned;
        }

        return cleaned;
    }

    private String parseMessageId(String response, String phoneNumber) {
        if (response == null || response.isBlank()) {
            return phoneNumber + "_" + System.currentTimeMillis();
        }

        if (response.toLowerCase().contains("success")) {
            String[] parts = response.split("[:|\\|]");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.matches("\\d+")) {
                    return trimmed;
                }
            }
        }

        if (response.matches("\\d+")) {
            return response;
        }

        return phoneNumber + "_" + System.currentTimeMillis();
    }

    @Override
    public String getProviderId() {
        return "smsozone-post";
    }

    @Override
    public int getPriority() {
        return config.getPriority();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }
}