package com.expensehub.webbackend.entity;

import com.expensehub.webbackend.security.EncryptedStringConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Corresponding to backlog Item 16: Financial personnel allocate travel budget ceilings by department and by period (annual/quarterly).
 * (department, periodType, periodLabel) uniquely determines a budget configuration,
 * periodLabel is in the format of "2026" (year) or "2026-Q1" (quarter).
 */
@Entity
@Table(
        name = "budget_configs",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"department", "period_type", "period_label"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private BudgetPeriodType periodType;

    @Column(name = "period_label", nullable = false)
    private String periodLabel;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String updatedBy;

    private Instant updatedAt;
}
