package com.vedvix.syncledger.notification.provider.email;

import com.vedvix.syncledger.notification.exception.ProviderException;
import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.domain.NotificationResult;
import com.vedvix.syncledger.notification.domain.Recipient;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import com.vedvix.syncledger.notification.template.TemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.providers.email.ses", name = "enabled", havingValue = "true")
public class SesEmailProvider implements EmailProvider {

    private final SesClient sesClient;
    private final TemplateEngine templateEngine;
    private final SesConfig config;

    @Override
    public NotificationResult send(NotificationRequest request) {
        log.info("Sending email notification {} using SES", request.id());

        try {
            for (Recipient recipient : request.recipients()) {
                if (recipient.email() == null || recipient.email().isBlank()) {
                    log.warn("Skipping recipient without email for notification {}", request.id());
                    continue;
                }

                String emailContent = templateEngine.process(
                        request.templateName(),
                        request.parameters()
                );

                SendEmailRequest emailRequest = SendEmailRequest.builder()
                        .source(config.getFromEmail())
                        .destination(Destination.builder()
                                .toAddresses(recipient.email())
                                .build())
                        .message(Message.builder()
                                .subject(Content.builder()
                                        .data(extractSubject(request))
                                        .build())
                                .body(Body.builder()
                                        .html(Content.builder()
                                                .data(emailContent)
                                                .build())
                                        .build())
                                .build())
                        .build();

                SendEmailResponse response = sesClient.sendEmail(emailRequest);

                log.info("Email sent successfully. Message ID: {}", response.messageId());

                return NotificationResult.builder()
                        .notificationId(request.id())
                        .recipientId(recipient.email())
                        .status(NotificationStatus.SENT)
                        .providerId(getProviderId())
                        .externalId(response.messageId())
                        .sentAt(Instant.now())
                        .attemptCount(1)
                        .build();
            }

            throw new ProviderException("NO_RECIPIENTS", "No valid recipients found", false);

        } catch (SesException e) {
            log.error("Failed to send email via SES: {}", e.getMessage(), e);

            boolean retryable = isRetryableError(e);
            throw new ProviderException(
                    "SES_ERROR",
                    "Failed to send email: " + e.awsErrorDetails().errorMessage(),
                    e,
                    retryable
            );
        }
    }

    @Override
    public String getProviderId() {
        return "ses";
    }

    @Override
    public int getPriority() {
        return config.getPriority();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    private String extractSubject(NotificationRequest request) {
        Object subject = request.parameters().get("subject");
        return subject != null ? subject.toString() : "Notification";
    }

    private boolean isRetryableError(SesException e) {
        if (e.statusCode() >= 500) {
            return true;
        }

        String errorCode = e.awsErrorDetails().errorCode();
        return "Throttling".equals(errorCode) ||
                "ServiceUnavailable".equals(errorCode);
    }
}