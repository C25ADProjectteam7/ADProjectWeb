package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.entity.ExpenseApprovalWorkflow;
import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.repository.UserRepository;
import com.expensehub.webbackend.service.ManagerExpenseService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for manager approval of expense reimbursements.
 * Exposes endpoints for managers to view and decide on expenses that exceed department budgets.
 */
@RestController
@RequestMapping("/api/manager/expense-approvals")
public class ManagerExpenseController {

    private final ManagerExpenseService managerExpenseService;
    private final UserRepository userRepository;

    public ManagerExpenseController(
        ManagerExpenseService managerExpenseService,
        UserRepository userRepository
    ) {
        this.managerExpenseService = managerExpenseService;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/manager/expense-approvals/pending
     * List expenses needing manager approval.
     * Role-based: any manager can see all pending expenses.
     */
    @GetMapping("/pending")
    public Page<ExpenseApprovalWorkflow> listPending(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String department
    ) {
        if (department != null && !department.isBlank()) {
            return managerExpenseService.listPendingApprovalsByDepartment(department, page, size);
        }
        return managerExpenseService.listPendingApprovals(page, size);
    }

    /**
     * GET /api/manager/expense-approvals/history
     * List expense approval history (decided by managers).
     */
    @GetMapping("/history")
    public Page<ExpenseApprovalWorkflow> listHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return managerExpenseService.listApprovalHistory(page, size);
    }

    /**
     * POST /api/manager/expense-approvals/{expenseId}/approve
     * Approve an expense as a manager.
     */
    @PostMapping("/{expenseId}/approve")
    public ResponseEntity<Void> approve(
        @PathVariable Long expenseId,
        @RequestBody Map<String, String> payload,
        Authentication authentication
    ) {
        Long managerId = resolveManagerId(authentication);
        String note = payload.getOrDefault("note", "");
        managerExpenseService.approveExpense(expenseId, managerId, note);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/manager/expense-approvals/{expenseId}/reject
     * Reject an expense as a manager.
     */
    @PostMapping("/{expenseId}/reject")
    public ResponseEntity<Void> reject(
        @PathVariable Long expenseId,
        @RequestBody Map<String, String> payload,
        Authentication authentication
    ) {
        Long managerId = resolveManagerId(authentication);
        String note = payload.getOrDefault("note", "");
        managerExpenseService.rejectExpense(expenseId, managerId, note);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/manager/expense-approvals/{expenseId}/decision
     * Check if manager has decided on an expense.
     */
    @GetMapping("/{expenseId}/decision")
    public ResponseEntity<Map<String, Object>> getDecision(@PathVariable Long expenseId) {
        Boolean decision = managerExpenseService.getManagerDecision(expenseId);

        // "No decision yet" is the normal case for anything sitting in the
        // pending queue, and it is exactly the case Map.of() rejects — it throws
        // NullPointerException on a null value. HashMap keeps the null so the
        // client can distinguish "not decided" from "rejected".
        Map<String, Object> response = new HashMap<>();
        response.put("hasDecision", decision != null);
        response.put("decision", decision);

        return ResponseEntity.ok(response);
    }

    /**
     * Resolve the acting manager's id from the authenticated JWT identity.
     */
    private Long resolveManagerId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository
            .findByEmailHash(User.sha256Hex(email.toLowerCase().trim()))
            .map(User::getId)
            .orElseThrow(() -> new AccessDeniedException(
                "Manager not found for email: " + email));
    }
}