package com.vedvix.syncledger.notification.provider.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vedvix.syncledger.notification.exception.ProviderException;
import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.domain.NotificationResult;
import com.vedvix.syncledger.notification.domain.Recipient;
import com.vedvix.syncledger.notification.dtos.SmsozoneApiResponse;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import com.vedvix.syncledger.notification.enums.SmsozoneErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.providers.sms.smsozone", name = "enabled", havingValue = "true")
public class SmsozoneSmsProvider implements SmsProvider {

    private final SmsozoneConfig config;
    private final RestTemplate restTemplate;

    @Override
    public NotificationResult send(NotificationRequest request) {
        log.info("Sending SMS notification {} using SMSOzone", request.id());

        try {
            for (Recipient recipient : request.recipients()) {
                if (recipient.phoneNumber() == null || recipient.phoneNumber().isBlank()) {
                    log.warn("Skipping recipient without phone number for notification {}", request.id());
                    continue;
                }

                String message = buildMessage(request);
                String messageId = sendSms(recipient.phoneNumber(), message);

                log.info("SMS sent successfully via SMSOzone. Message ID: {}", messageId);

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

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Failed to send SMS via SMSOzone: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);

            boolean retryable = isRetryableError(e);
            throw new ProviderException(
                    "SMSOZONE_ERROR",
                    "Failed to send SMS: " + e.getMessage(),
                    e,
                    retryable
            );
        } catch (Exception e) {
            log.error("Unexpected error sending SMS via SMSOzone: {}", e.getMessage(), e);
            throw new ProviderException(
                    "SMSOZONE_ERROR",
                    "Failed to send SMS: " + e.getMessage(),
                    e,
                    true // Retry on unexpected errors
            );
        }
    }

    private String sendSms(String phoneNumber, String message) {

        String cleanedNumber = cleanPhoneNumber(phoneNumber);
        String encodedMessage = URLEncoder.encode(
                message,
                StandardCharsets.UTF_8
        );
        String url = UriComponentsBuilder.fromHttpUrl(config.getApiUrl())
                .queryParam("APIKey", config.getApiKey())
                .queryParam("senderid", config.getSenderId())
                .queryParam("channel", "Trans")
                .queryParam("DCS", "0")
                .queryParam("flashsms", "0")
                .queryParam("number", cleanedNumber)
                .queryParam("text", encodedMessage)
                .queryParam("route", "2069")
                .build(true)
                .toUriString();

        log.debug("Sending SMS to SMSOzone: {}",
                url.replaceAll("APIKey=[^&]*", "APIKey=***"));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN
        ));

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new ProviderException(
                    "SMSOZONE_NO_RESPONSE",
                    "No response from SMSOzone",
                    true
            );
        }

        String rawResponse = response.getBody();
        log.info("SMSOzone raw response: {}", rawResponse);

        // Try JSON parse
        try {
            ObjectMapper mapper = new ObjectMapper();
            SmsozoneApiResponse body =
                    mapper.readValue(rawResponse, SmsozoneApiResponse.class);

            SmsozoneErrorCode error = SmsozoneErrorCode.from(body.getErrorCode());

            if (!body.isSuccess()) {
                throw new ProviderException(
                        "SMSOZONE_" + error.name(),
                        body.getErrorMessage(),
                        error.isRetryable()
                );
            }

            if (body.getMessageData() != null && !body.getMessageData().isEmpty()) {
                return body.getMessageData().get(0).getMessageId();
            }

            return "SMSOZONE_" + cleanedNumber + "_" + body.getJobId();

        } catch (Exception ex) {
            // Non-JSON but SMS might still be delivered
            log.warn("Non-JSON SMSOzone response, treating as success");
            return "SMSOZONE_" + cleanedNumber + "_" + System.currentTimeMillis();
        }
    }


    private String buildMessage(NotificationRequest request) {
        Object message = request.parameters().get("message");
        if (message != null) {
            return message.toString();
        }

        // Check for OTP message
        Object otp = request.parameters().get("otp");
        if (otp != null) {
            return String.format(
                    "KAPS Your OTP is %s. Please use this code to complete your verification. Do not share this OTP with anyone.",
                    otp
            );
        }

        // Simple template processing
        String template = (String) request.parameters().getOrDefault(
                "template",
                "KAPS: Hello {{firstName}}, {{message}}"
        );

        // Replace variables
        for (var entry : request.parameters().entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}",
                    String.valueOf(entry.getValue()));
        }

        return template;
    }

    private String cleanPhoneNumber(String phoneNumber) {
        // Remove all non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");

        // If it starts with +91, keep it
        if (cleaned.startsWith("+91")) {
            return cleaned;
        }

        // If it starts with 91, add +
        if (cleaned.startsWith("91") && cleaned.length() == 12) {
            return "+" + cleaned;
        }

        // If it's just 10 digits, add +91
        if (cleaned.length() == 10) {
            return "+91" + cleaned;
        }

        // Return as is if already in correct format
        return cleaned;
    }

    private String parseMessageId(String response, String phoneNumber) {
        // SMSOzone typically returns success messages in various formats
        // Common formats:
        // "Success: Message sent successfully. MessageId: 12345"
        // "12345" (just the message ID)
        // "Success|12345"

        if (response == null || response.isBlank()) {
            return phoneNumber + "_" + System.currentTimeMillis();
        }

        // Try to extract message ID from response
        if (response.toLowerCase().contains("success")) {
            // Try to find numeric message ID
            String[] parts = response.split("[:|\\|]");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.matches("\\d+")) {
                    return trimmed;
                }
            }
        }

        // If response is just a number, return it
        if (response.matches("\\d+")) {
            return response;
        }

        // Generate a fallback ID
        return phoneNumber + "_" + System.currentTimeMillis();
    }

    private boolean isRetryableError(Exception e) {
        if (e instanceof HttpServerErrorException) {
            // Retry on server errors (5xx)
            return true;
        }

        if (e instanceof HttpClientErrorException clientError) {
            int statusCode = clientError.getStatusCode().value();

            // Retry on rate limiting (429)
            if (statusCode == 429) {
                return true;
            }

            // Don't retry on client errors (4xx) except 429
            return false;
        }

        // Retry on other exceptions
        return true;
    }

    @Override
    public String getProviderId() {
        return "smsozone";
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