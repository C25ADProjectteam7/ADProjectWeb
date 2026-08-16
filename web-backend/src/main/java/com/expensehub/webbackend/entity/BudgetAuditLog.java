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

// Corresponds to backlog Item 16, acceptance criteria: "All budget changes are logged for audit."
@Entity
@Table(name = "budget_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long budgetId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String department;

    @Column(nullable = false)
    private String periodLabel;

    private BigDecimal previousAmount;

    @Column(nullable = false)
    private BigDecimal newAmount;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String changedBy;

    @Column(nullable = false)
    private Instant changedAt;
}
