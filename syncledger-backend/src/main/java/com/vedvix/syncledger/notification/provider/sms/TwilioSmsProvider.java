package com.vedvix.syncledger.notification.provider.sms;

import com.vedvix.syncledger.notification.exception.ProviderException;
import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.domain.NotificationResult;
import com.vedvix.syncledger.notification.domain.Recipient;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.providers.sms.twilio", name = "enabled", havingValue = "true")
public class TwilioSmsProvider implements SmsProvider {

    private final TwilioConfig config;

    @PostConstruct
    public void init() {
        Twilio.init(config.getAccountSid(), config.getAuthToken());
    }

    @Override
    public NotificationResult send(NotificationRequest request) {
        log.info("Sending SMS notification {} using Twilio", request.id());

        try {
            for (Recipient recipient : request.recipients()) {
                if (recipient.phoneNumber() == null || recipient.phoneNumber().isBlank()) {
                    log.warn("Skipping recipient without phone number for notification {}", request.id());
                    continue;
                }

                String messageBody = buildMessage(request);

                Message message = Message.creator(
                        new PhoneNumber(recipient.phoneNumber()),
                        new PhoneNumber(config.getFromNumber()),
                        messageBody
                ).create();

                log.info("SMS sent successfully. SID: {}", message.getSid());

                return NotificationResult.builder()
                        .notificationId(request.id())
                        .recipientId(recipient.phoneNumber())
                        .status(NotificationStatus.SENT)
                        .providerId(getProviderId())
                        .externalId(message.getSid())
                        .sentAt(Instant.now())
                        .attemptCount(1)
                        .build();
            }

            throw new ProviderException("NO_RECIPIENTS", "No valid recipients found", false);

        } catch (ApiException e) {
            log.error("Failed to send SMS via Twilio: {}", e.getMessage(), e);

            boolean retryable = isRetryableError(e);
            throw new ProviderException(
                    "TWILIO_ERROR",
                    "Failed to send SMS: " + e.getMessage(),
                    e,
                    retryable
            );
        }
    }

    @Override
    public String getProviderId() {
        return "twilio";
    }

    @Override
    public int getPriority() {
        return config.getPriority();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    private String buildMessage(NotificationRequest request) {
        Object message = request.parameters().get("message");
        if (message != null) {
            return message.toString();
        }

        // Simple template processing
        String template = (String) request.parameters().getOrDefault(
                "template",
                "Hello {{firstName}}, this is a notification from {{companyName}}"
        );

        for (var entry : request.parameters().entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}",
                    String.valueOf(entry.getValue()));
        }

        return template;
    }

    private boolean isRetryableError(ApiException e) {
        int statusCode = e.getStatusCode();
        return statusCode >= 500 || statusCode == 429; // Server errors or rate limits
    }
}