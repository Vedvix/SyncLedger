package com.vedvix.syncledger.payment.service;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AppStartTime {

    private final Instant startedAt = Instant.now();

    public Instant getStartedAt() {
        return startedAt;
    }
}
