package com.expensehub.webbackend.config;

import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.BudgetPeriodType;
import com.expensehub.webbackend.repository.BudgetConfigRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
@Profile("dev")
public class FinanceDemoDataSeeder {

    @Bean
    CommandLineRunner seedFinanceDemoData(BudgetConfigRepository budgetConfigRepository) {
        return args -> {
            if (budgetConfigRepository.count() > 0) {
                return;
            }

            budgetConfigRepository.save(
                    BudgetConfig.builder()
                            .department("Engineering")
                            .periodType(BudgetPeriodType.QUARTERLY)
                            .periodLabel("2026-Q1")
                            .amount(new BigDecimal("5000.00"))
                            .updatedBy("seed")
                            .updatedAt(Instant.now())
                            .build());

            budgetConfigRepository.save(
                    BudgetConfig.builder()
                            .department("Sales")
                            .periodType(BudgetPeriodType.ANNUAL)
                            .periodLabel("2026")
                            .amount(new BigDecimal("20000.00"))
                            .updatedBy("seed")
                            .updatedAt(Instant.now())
                            .build());
        };
    }
}
