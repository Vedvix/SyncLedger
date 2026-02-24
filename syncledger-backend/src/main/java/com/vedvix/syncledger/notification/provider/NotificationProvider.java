package com.vedvix.syncledger.notification.provider;

import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.domain.NotificationResult;

public interface NotificationProvider {
    NotificationResult send(NotificationRequest request);
    String getProviderId();
    int getPriority();
    boolean isEnabled();
}