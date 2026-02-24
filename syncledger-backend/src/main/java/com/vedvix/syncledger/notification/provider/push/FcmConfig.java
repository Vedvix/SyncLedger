package com.vedvix.syncledger.notification.provider.push;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "notification.providers.push.fcm")
@ConditionalOnProperty(prefix = "notification.providers.push.fcm", name = "enabled", havingValue = "true")
public class FcmConfig {

    private boolean enabled;
    private int priority = 1;
    private String serviceAccountKeyPath;
}
