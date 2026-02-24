package com.vedvix.syncledger.notification.provider.email;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ses.SesClient;

@Data
@Configuration
@ConfigurationProperties(prefix = "notification.providers.email.ses")
@ConditionalOnProperty(prefix = "notification.providers.email.ses", name = "enabled", havingValue = "true")
public class SesConfig {

    private boolean enabled;
    private int priority = 1;
    private String fromEmail;
    private String region = "us-east-1";

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(software.amazon.awssdk.regions.Region.of(region))
                .build();
    }
}
