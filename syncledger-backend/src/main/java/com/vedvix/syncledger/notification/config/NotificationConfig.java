package com.vedvix.syncledger.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    @Bean
    public ObjectMapper notificationObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules(); // Register JSR310 (Java 8 date/time) module
    }
}