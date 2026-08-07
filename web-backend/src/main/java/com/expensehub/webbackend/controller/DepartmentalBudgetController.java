package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.entity.DepartmentalBudget;
import com.expensehub.webbackend.service.DepartmentalBudgetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/budgets")
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