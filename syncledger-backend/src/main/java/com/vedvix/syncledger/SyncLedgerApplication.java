package com.vedvix.syncledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SyncLedger - Invoice Processing Portal
 * Main entry point for the application.
 * 
 * This is a Spring Boot application that uses @SpringBootApplication to enable auto-configuration and component scanning.
 * Scheduling is enabled via @EnableScheduling for scheduled tasks (email polling).
 *
 * @author vedvix
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class SyncLedgerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncLedgerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SyncLedgerApplication.class, args);
        LOGGER.info("""
            
            ╔═══════════════════════════════════════════════════════════════╗
            ║                                                               ║
            ║   ███████╗██╗   ██╗███╗   ██╗ ██████╗██╗     ███████╗██████╗  ║
            ║   ██╔════╝╚██╗ ██╔╝████╗  ██║██╔════╝██║     ██╔════╝██╔══██╗ ║
            ║   ███████╗ ╚████╔╝ ██╔██╗ ██║██║     ██║     █████╗  ██║  ██║ ║
            ║   ╚════██║  ╚██╔╝  ██║╚██╗██║██║     ██║     ██╔══╝  ██║  ██║ ║
            ║   ███████║   ██║   ██║ ╚████║╚██████╗███████╗███████╗██████╔╝ ║
            ║   ╚══════╝   ╚═╝   ╚═╝  ╚═══╝ ╚═════╝╚══════╝╚══════╝╚═════╝  ║
            ║                                                               ║
            ║                    by vedvix                                  ║
            ╠═══════════════════════════════════════════════════════════════╣
            ║  ✅ Backend Started Successfully!                             ║
            ║  📖 API Docs: http://localhost:8080/api/swagger-ui.html       ║
            ║  🔧 Health:   http://localhost:8080/api/actuator/health       ║
            ╚═══════════════════════════════════════════════════════════════╝
            """);
    }

    @Bean
    public CommandLineRunner logEmailPollingEnabled(@Value("${email.polling.enabled:false}") boolean enabled) {
        return args -> LOGGER.info("email.polling.enabled = {}", enabled);
    }
}
