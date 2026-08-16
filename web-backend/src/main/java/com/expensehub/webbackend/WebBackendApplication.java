package com.expensehub.webbackend;

import com.expensehub.webbackend.security.EncryptedStringConverter;
import com.expensehub.webbackend.security.EncryptionService;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling is required for ManagerService's @Scheduled auto-sync job
// (Item 20) to actually fire; without it Spring silently ignores @Scheduled.
// Approval lives on the MOBILE data source (see MobileDataSourceConfig) and
// in the com.expensehub.webbackend.mobile.entity package - the primary (web)
// EntityManager only scans com.expensehub.webbackend.entity, so it never sees
// it (and won't try to validate the approvals table against the web DB).
@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = "com.expensehub.webbackend.entity")
public class WebBackendApplication {

    private final EncryptionService encryptionService;

    public WebBackendApplication(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /**
     * Wires the {@link EncryptionService} into the JPA {@link EncryptedStringConverter}.
     * JPA converters are instantiated by Hibernate (not Spring), so they cannot
     * {@code @Inject} beans directly — this static bridge is called once at startup.
     */
    @PostConstruct
    public void initEncryptionConverter() {
        EncryptedStringConverter.setEncryptionService(encryptionService);
    }

    public static void main(String[] args) {
        SpringApplication.run(WebBackendApplication.class, args);
    }
}
