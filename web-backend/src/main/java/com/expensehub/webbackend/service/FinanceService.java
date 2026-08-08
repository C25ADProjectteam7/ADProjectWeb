package com.expensehub.webbackend.service;

import com.expensehub.webbackend.dto.AuditEntryResponse;
import com.expensehub.webbackend.dto.BudgetConfigRequest;
import com.expensehub.webbackend.dto.BudgetConfigResponse;
import com.expensehub.webbackend.dto.ReimbursementResponse;
import com.expensehub.webbackend.dto.ReimbursementReviewRequest;
import com.expensehub.webbackend.entity.ReimbursementCategory;
import com.expensehub.webbackend.entity.ReimbursementStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface FinanceService {

    // Budget allocation (Item 16)
    List<BudgetConfigResponse> listBudgets(String department);

    BudgetConfigResponse upsertBudget(BudgetConfigRequest request, String actor);

    List<AuditEntryResponse> getBudgetAudit(Long budgetId);

    // Reimbursement requests
    Page<ReimbursementResponse> listReimbursements(
            ReimbursementStatus status,
            String department,
            ReimbursementCategory category,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    ReimbursementResponse getReimbursement(Long id);


    ReimbursementResponse reviewReimbursement(
            Long id, ReimbursementReviewRequest request, String actor);

    List<AuditEntryResponse> getReimbursementAudit(Long id);

    //EXPORT (Item 18)
    byte[] exportReimbursementsToExcel(
            ReimbursementStatus status,
            String department,
            ReimbursementCategory category,
            LocalDate from,
            LocalDate to);
}
