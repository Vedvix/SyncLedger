package com.vedvix.syncledger.notification.domain;

import java.util.Map;

public record Recipient(
        String email,
        String phoneNumber,
        String deviceToken,
        Map<String, Object> attributes
) {
    public Recipient {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}