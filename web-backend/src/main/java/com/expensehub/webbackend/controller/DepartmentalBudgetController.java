package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.entity.DepartmentalBudget;
import com.expensehub.webbackend.service.DepartmentalBudgetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Corresponds to backlog Item 20 (Manager Approval Notifications): the yearly
 * per-department budget cap used to decide whether a trip needs manager approval.
 *
 * <p>This is intentionally separate from FinanceController's /api/finance/budgets
 * (Item 16, quarterly/annual reimbursement budgets) — different backlog item,
 * different semantics (trip pre-approval cap vs. reimbursement spend limit) — so
 * it lives under /api/manager/** instead of colliding with the finance route.
 */
@RestController
@RequestMapping("/api/manager/budgets")
public class DepartmentalBudgetController {

    private final DepartmentalBudgetService budgetService;

    public DepartmentalBudgetController(
            DepartmentalBudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<DepartmentalBudget> listBudgets(
            @RequestParam(required = false) Integer year) {

        return budgetService.listBudgets(year);
    }

    @PostMapping
    public DepartmentalBudget createBudget(
            @RequestBody DepartmentalBudget budget) {

        return budgetService.createBudget(budget);
    }

    @PutMapping("/{id}")
    public DepartmentalBudget updateBudget(
            @PathVariable Long id,
            @RequestBody DepartmentalBudget budget) {

        return budgetService.updateBudget(id, budget);
    }
}