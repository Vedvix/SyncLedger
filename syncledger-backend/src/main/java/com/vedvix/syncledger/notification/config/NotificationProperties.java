package com.vedvix.syncledger.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private RateLimiting rateLimiting = new RateLimiting();
    private Retry retry = new Retry();
    private Providers providers = new Providers();

    @Data
    public static class RateLimiting {
        private boolean enabled = true;
        private long globalLimit = 10000;
        private long perCustomerLimit = 1000;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private String initialDelay = "1s";
        private double multiplier = 2.0;
        private String maxDelay = "10m";
    }

    @Data
    public static class Providers {
        private Email email = new Email();
        private Sms sms = new Sms();
        private Push push = new Push();
    }

    @Data
    public static class Email {
        private Ses ses = new Ses();
    }

    @Data
    public static class Sms {
        private Twilio twilio = new Twilio();
    }

    @Data
    public static class Push {
        private Fcm fcm = new Fcm();
    }

    @Data
    public static class Ses {
        private boolean enabled = false;
        private int priority = 1;
        private String region = "us-east-1";
        private String fromEmail;
    }

    @Data
    public static class Twilio {
        private boolean enabled = false;
        private int priority = 1;
    }

    @Data
    public static class Fcm {
        private boolean enabled = false;
        private int priority = 1;
    }
}