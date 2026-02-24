package com.vedvix.syncledger.notification.service;

import com.vedvix.syncledger.notification.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for direct notification sending without going through REST API.
 * Use this service when you need to send notifications from within your application code.
 *
 * Example usage:
 * <pre>
 * &#64;Autowired
 * private DirectNotificationService directNotificationService;
 *
 * public void someBusinessLogic() {
 *     NotificationRequest request = NotificationRequest.builder()
 *         .channel(NotificationChannel.EMAIL)
 *         .templateName("welcome_email")
 *         .recipients(List.of(new Recipient("user@example.com", null, null, Map.of())))
 *         .parameters(Map.of("companyName", "ACME"))
 *         .build();
 *
 *     String notificationId = directNotificationService.sendNotificationDirectly(request);
 * }
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectNotificationService {

    private final NotificationService notificationService;

    /**
     * Send notification directly by pushing to queue
     * @param request The notification request
     * @return The notification ID
     */
    public String sendNotificationDirectly(NotificationRequest request) {
        log.info("Sending notification directly: {}", request.id());
        NotificationRequest result = notificationService.sendNotification(request);
        return result.id();
    }

    /**
     * Send multiple notifications in batch
     * @param requests List of notification requests
     * @return List of notification IDs
     */
    public List<String> sendBatchNotificationsDirectly(List<NotificationRequest> requests) {
        log.info("Sending {} notifications directly", requests.size());
        return notificationService.sendBatchNotifications(requests)
                .stream()
                .map(NotificationRequest::id)
                .toList();
    }
}