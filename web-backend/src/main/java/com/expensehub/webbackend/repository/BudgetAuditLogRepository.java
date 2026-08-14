package com.expensehub.webbackend.repository;

import com.expensehub.webbackend.entity.BudgetAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetAuditLogRepository extends JpaRepository<BudgetAuditLog, Long> {

    List<BudgetAuditLog> findByBudgetIdOrderByChangedAtDesc(Long budgetId);
}
