package com.expensehub.webbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Declares the PRIMARY (web database) JPA repositories explicitly. Once any
 * user-defined @EnableJpaRepositories exists (see
 * MobileJpaRepositoriesConfig), Spring Boot's auto-configured repository scan
 * backs off - without this class the web entities' repositories (User, budget,
 * reimbursement...) would not be created at all.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.expensehub.webbackend.repository",
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager")
public class PrimaryJpaRepositoriesConfig {
}
