package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.dto.AuditEntryResponse;
import com.expensehub.webbackend.dto.BudgetConfigRequest;
import com.expensehub.webbackend.dto.BudgetConfigResponse;
import com.expensehub.webbackend.dto.ReimbursementResponse;
import com.expensehub.webbackend.dto.ReimbursementReviewRequest;
import com.expensehub.webbackend.entity.ReimbursementCategory;
import com.expensehub.webbackend.entity.ReimbursementStatus;
import com.expensehub.webbackend.service.FinanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Corresponding to backlog items 16-19: budget allocation, reimbursement request review, and data export for financial personnel.
 * Permissions have been restricted to ADMIN / FINANCE_STAFF (/api/finance/**) in SecurityConfig.
 */
@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    // Budget allocation (Item 16)

    @GetMapping("/budgets")
    public List<BudgetConfigResponse> listBudgets(
            @RequestParam(required = false) String department) {
        return financeService.listBudgets(department);
    }

    @PutMapping("/budgets")
    public BudgetConfigResponse upsertBudget(
            @Valid @RequestBody BudgetConfigRequest request, Authentication authentication) {
        return financeService.upsertBudget(request, actorOf(authentication));
    }

    @GetMapping("/budgets/{id}/audit")
    public List<AuditEntryResponse> getBudgetAudit(@PathVariable Long id) {
        return financeService.getBudgetAudit(id);
    }

    // Reimbursement requests (Item 17, 19)

    @GetMapping("/reimbursements")
    public Page<ReimbursementResponse> listReimbursements(
            @RequestParam(required = false) ReimbursementStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) ReimbursementCategory category,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            Pageable pageable) {
        return financeService.listReimbursements(status, department, category, from, to, pageable);
    }

    @GetMapping("/reimbursements/{id}")
    public ReimbursementResponse getReimbursement(@PathVariable Long id) {
        return financeService.getReimbursement(id);
    }

    @PatchMapping("/reimbursements/{id}/review")
    public ReimbursementResponse reviewReimbursement(
            @PathVariable Long id,
            @Valid @RequestBody ReimbursementReviewRequest request,
            Authentication authentication) {
        return financeService.reviewReimbursement(id, request, actorOf(authentication));
    }

    @GetMapping("/reimbursements/{id}/audit")
    public List<AuditEntryResponse> getReimbursementAudit(@PathVariable Long id) {
        return financeService.getReimbursementAudit(id);
    }

    //Data export (Item 18)

    @GetMapping("/reimbursements/export")
    public ResponseEntity<byte[]> exportReimbursements(
            @RequestParam(required = false) ReimbursementStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) ReimbursementCategory category,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {
        byte[] file = financeService.exportReimbursementsToExcel(status, department, category, from, to);
        String filename =
                "reimbursements-" + DateTimeFormatter.ISO_DATE.format(LocalDate.now()) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(file);
    }

    private String actorOf(Authentication authentication) {
        return authentication == null ? "unknown" : authentication.getName();
    }
}
