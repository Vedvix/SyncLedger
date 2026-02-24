package com.vedvix.syncledger.notification.enums;

public enum NotificationStatus {
    PENDING,
    QUEUED,
    PROCESSING,
    SENT,
    DELIVERED,
    FAILED,
    RETRY_SCHEDULED,
    CANCELLED,
    EXPIRED
}