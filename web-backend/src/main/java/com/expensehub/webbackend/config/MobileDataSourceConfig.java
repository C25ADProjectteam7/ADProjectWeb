package com.expensehub.webbackend.config;

import com.expensehub.webbackend.mobile.entity.Approval;
import com.expensehub.webbackend.mobile.repository.ApprovalRepository;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Second data source pointing at the MOBILE MySQL instance - the Manager
 * approval workflow (backlog Item 20) now reads and writes the approvals
 * table directly in Mobile's database, so both the Web manager view and the
 * Mobile app see the SAME data (team decision: one shared SQL source instead
 * of a web-local copy that drifted out of sync).
 *
 * Only the mobile Approval entity/repository live here (see
 * MobileJpaRepositoriesConfig); everything else stays on the primary (web)
 * data source. The decide() flow also flips the Mobile trips.status row
 * directly (same database), which is what the app renders on the trips list.
 */
@Configuration
public class MobileDataSourceConfig {

    @Bean(name = "mobileDataSource")
    public DataSource mobileDataSource(
            @Value("${mobile.datasource.url}") String url,
            @Value("${mobile.datasource.username}") String username,
            @Value("${mobile.datasource.password}") String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setPoolName("mobile-db-pool");
        ds.setMaximumPoolSize(5);
        return ds;
    }

    @Bean(name = "mobileEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean mobileEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("mobileDataSource") DataSource mobileDataSource) {
        return builder
                .dataSource(mobileDataSource)
                .packages(Approval.class)
                .persistenceUnit("mobile")
                .build();
    }

    @Bean(name = "mobileTransactionManager")
    public PlatformTransactionManager mobileTransactionManager(
            @Qualifier("mobileEntityManagerFactory") EntityManagerFactory mobileEntityManagerFactory) {
        return new JpaTransactionManager(mobileEntityManagerFactory);
    }

    /** For raw SQL against Mobile's trips table (status flip on approve/reject). */
    @Bean(name = "mobileJdbcTemplate")
    public JdbcTemplate mobileJdbcTemplate(@Qualifier("mobileDataSource") DataSource mobileDataSource) {
        return new JdbcTemplate(mobileDataSource);
    }
}
