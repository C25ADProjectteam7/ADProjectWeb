package com.expensehub.webbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling is required for ManagerService's @Scheduled auto-sync job
// (Item 20) to actually fire; without it Spring silently ignores @Scheduled.
@SpringBootApplication
@EnableScheduling
public class WebBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebBackendApplication.class, args);
    }
}
