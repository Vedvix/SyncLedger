package com.vedvix.syncledger.notification.provider.sms;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "notification.providers.sms.twilio")
@ConditionalOnProperty(prefix = "notification.providers.sms.twilio", name = "enabled", havingValue = "true")
public class TwilioConfig {

    private boolean enabled;
    private int priority = 1;
    private String accountSid;
    private String authToken;
    private String fromNumber;
}
