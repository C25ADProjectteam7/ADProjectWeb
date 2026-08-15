package com.expensehub.webbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA repositories for the MOBILE (shared-SQL) data source - currently just
 * the approval workflow's ApprovalRepository. Entity manager and transaction
 * manager beans come from MobileDataSourceConfig.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.expensehub.webbackend.mobile.repository",
        entityManagerFactoryRef = "mobileEntityManagerFactory",
        transactionManagerRef = "mobileTransactionManager")
public class MobileJpaRepositoriesConfig {
}
