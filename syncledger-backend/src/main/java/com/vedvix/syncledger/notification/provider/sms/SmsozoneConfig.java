package com.vedvix.syncledger.notification.provider.sms;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Data
@Configuration
@ConfigurationProperties(prefix = "notification.providers.sms.smsozone")
@ConditionalOnProperty(prefix = "notification.providers.sms.smsozone", name = "enabled", havingValue = "true")
public class SmsozoneConfig {

    private boolean enabled;
    private int priority = 2; // Higher priority than Twilio by default
    private String apiUrl = "https://smsozone.com/api/mt/SendSMS"; // Default URL
    private String apiKey;
    private String password;
    private String senderId;
    private String route;

    @Bean
    public RestTemplate smsRestTemplate() {
        return new RestTemplate();
    }
}