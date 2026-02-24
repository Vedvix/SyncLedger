package com.vedvix.syncledger.payment.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "order")
@Data
public class OrderProperties {
    private int expiryMinutes;
}
