package com.expensehub.webbackend.config;

import com.expensehub.webbackend.entity.BudgetAuditLog;
import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.DepartmentalBudget;
import com.expensehub.webbackend.entity.ExpenseApprovalWorkflow;
import com.expensehub.webbackend.entity.ReimbursementAuditLog;
import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.mobile.entity.Approval;
import com.expensehub.webbackend.mobile.repository.ApprovalRepository;
import com.expensehub.webbackend.repository.BudgetAuditLogRepository;
import com.expensehub.webbackend.repository.BudgetConfigRepository;
import com.expensehub.webbackend.repository.DepartmentalBudgetRepository;
import com.expensehub.webbackend.repository.ExpenseApprovalWorkflowRepository;
import com.expensehub.webbackend.repository.ReimbursementAuditLogRepository;
import com.expensehub.webbackend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time migration that re-saves all existing entities containing PII fields.
 * <p>
 * When JPA {@code @Convert} annotations are applied to existing plaintext data,
 * the {@link com.expensehub.webbackend.security.EncryptedStringConverter}
 * gracefully falls back to returning plaintext on read, but data is only
 * encrypted on write. This migration loads every entity and saves it again,
 * which triggers the converter to encrypt all PII fields.
 * <p>
 * Idempotent: checks whether any user has an {@code emailHash} set (the hash
 * column is populated by {@code @PrePersist} and only exists after the first
 * save with the new entity definition). Once any user has {@code emailHash},
 * all data is considered migrated.
 * <p>
 * Runs ONLY in the {@code prod} profile (not during dev or tests).
 */
@Configuration
@Profile("prod")
public class DataEncryptionMigration {

    private static final Logger log = LoggerFactory.getLogger(DataEncryptionMigration.class);

    private final UserRepository userRepository;
    private final ApprovalRepository approvalRepository;
    private final ExpenseApprovalWorkflowRepository workflowRepository;
    private final BudgetConfigRepository budgetConfigRepository;
    private final DepartmentalBudgetRepository departmentalBudgetRepository;
    private final BudgetAuditLogRepository budgetAuditLogRepository;
    private final ReimbursementAuditLogRepository reimbursementAuditLogRepository;

    public DataEncryptionMigration(
            UserRepository userRepository,
            ApprovalRepository approvalRepository,
            ExpenseApprovalWorkflowRepository workflowRepository,
            BudgetConfigRepository budgetConfigRepository,
            DepartmentalBudgetRepository departmentalBudgetRepository,
            BudgetAuditLogRepository budgetAuditLogRepository,
            ReimbursementAuditLogRepository reimbursementAuditLogRepository) {
        this.userRepository = userRepository;
        this.approvalRepository = approvalRepository;
        this.workflowRepository = workflowRepository;
        this.budgetConfigRepository = budgetConfigRepository;
        this.departmentalBudgetRepository = departmentalBudgetRepository;
        this.budgetAuditLogRepository = budgetAuditLogRepository;
        this.reimbursementAuditLogRepository = reimbursementAuditLogRepository;
    }

    @PostConstruct
    @Transactional
    public void migrate() {
        // Check if migration has already been applied.
        // The emailHash column is only populated by @PrePersist on User after
        // migration; if any user has it, all data is considered migrated.
        boolean alreadyMigrated = userRepository.count() > 0
                && userRepository.findAll().stream()
                        .anyMatch(u -> u.getEmailHash() != null);

        if (alreadyMigrated) {
            log.info("Data encryption migration already applied — skipping.");
            return;
        }

        log.info("Starting data encryption migration for existing PII data...");

        // Re-save Users (triggers @Convert for email/fullName/department and
        // @PrePersist for emailHash)
        int userCount = 0;
        for (User user : userRepository.findAll()) {
            userRepository.save(user);
            userCount++;
        }
        log.info("Migrated {} User records.", userCount);

        // Re-save Approvals (triggers @Convert for PII fields)
        int approvalCount = 0;
        for (Approval approval : approvalRepository.findAll()) {
            approvalRepository.save(approval);
            approvalCount++;
        }
        log.info("Migrated {} Approval records.", approvalCount);

        // Re-save ExpenseApprovalWorkflows
        int workflowCount = 0;
        for (ExpenseApprovalWorkflow wf : workflowRepository.findAll()) {
            workflowRepository.save(wf);
            workflowCount++;
        }
        log.info("Migrated {} ExpenseApprovalWorkflow records.", workflowCount);

        // Re-save BudgetConfigs
        int budgetConfigCount = 0;
        for (BudgetConfig bc : budgetConfigRepository.findAll()) {
            budgetConfigRepository.save(bc);
            budgetConfigCount++;
        }
        log.info("Migrated {} BudgetConfig records.", budgetConfigCount);

        // Re-save DepartmentalBudgets
        int deptBudgetCount = 0;
        for (DepartmentalBudget db : departmentalBudgetRepository.findAll()) {
            departmentalBudgetRepository.save(db);
            deptBudgetCount++;
        }
        log.info("Migrated {} DepartmentalBudget records.", deptBudgetCount);

        // Re-save BudgetAuditLogs
        int auditLogCount = 0;
        for (BudgetAuditLog bl : budgetAuditLogRepository.findAll()) {
            budgetAuditLogRepository.save(bl);
            auditLogCount++;
        }
        log.info("Migrated {} BudgetAuditLog records.", auditLogCount);

        // Re-save ReimbursementAuditLogs
        int reimbAuditCount = 0;
        for (ReimbursementAuditLog rl : reimbursementAuditLogRepository.findAll()) {
            reimbursementAuditLogRepository.save(rl);
            reimbAuditCount++;
        }
        log.info("Migrated {} ReimbursementAuditLog records.", reimbAuditCount);

        log.info("Data encryption migration complete.");
    }
}