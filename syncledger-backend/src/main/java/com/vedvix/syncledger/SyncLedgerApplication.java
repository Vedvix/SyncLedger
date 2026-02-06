package com.vedvix.syncledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SyncLedger - Invoice Processing Portal
 * Main entry point for the application.
 * 
 * @author vedvix
 * @version 1.0.0
 * 
 * @SpringBootApplication - Combines @Configuration, @EnableAutoConfiguration, @ComponentScan
 * @EnableScheduling - Enables scheduled tasks (email polling)
 */
@SpringBootApplication
@EnableScheduling
public class SyncLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncLedgerApplication.class, args);
        System.out.println("""
            
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
            ║  📖 API Docs: http://localhost:8080/swagger-ui.html           ║
            ║  🔧 Health:   http://localhost:8080/actuator/health           ║
            ╚═══════════════════════════════════════════════════════════════╝
            """);
    }
}
