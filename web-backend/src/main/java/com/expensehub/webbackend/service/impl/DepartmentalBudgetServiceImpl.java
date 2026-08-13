package com.expensehub.webbackend.service.impl;

import com.expensehub.webbackend.entity.DepartmentalBudget;
import com.expensehub.webbackend.repository.DepartmentalBudgetRepository;
import com.expensehub.webbackend.service.DepartmentalBudgetService;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DepartmentalBudgetServiceImpl
        implements DepartmentalBudgetService {

    private final DepartmentalBudgetRepository budgetRepository;

    public DepartmentalBudgetServiceImpl(
            DepartmentalBudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Override
    public List<DepartmentalBudget> listBudgets(Integer year) {

        if (year != null) {
            return budgetRepository.findByBudgetYear(year);
        }

        return budgetRepository.findAll();
    }

    @Override
    public DepartmentalBudget createBudget(
            DepartmentalBudget budget) {

        if (budgetRepository
                .findByDepartmentAndBudgetYear(
                        budget.getDepartment(),
                        budget.getBudgetYear())
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Budget already exists for this department and year");
        }

        return budgetRepository.save(budget);
    }

    @Override
    public DepartmentalBudget updateBudget(
            Long id,
            DepartmentalBudget budget) {

        DepartmentalBudget existing =
                budgetRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Budget not found"));

        existing.setDepartment(budget.getDepartment());
        existing.setBudgetYear(budget.getBudgetYear());
        existing.setBudgetAmount(budget.getBudgetAmount());

        return budgetRepository.save(existing);
    }

    @Override
    public Optional<BigDecimal> getLimit(String department) {
        return budgetRepository
                .findByDepartmentAndBudgetYear(department, Year.now().getValue())
                .map(DepartmentalBudget::getBudgetAmount);
    }
}