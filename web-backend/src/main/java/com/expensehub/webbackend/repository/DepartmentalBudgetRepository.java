package com.expensehub.webbackend.repository;

import com.expensehub.webbackend.entity.DepartmentalBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentalBudgetRepository
        extends JpaRepository<DepartmentalBudget, Long> {

    List<DepartmentalBudget> findByBudgetYear(Integer budgetYear);

    Optional<DepartmentalBudget> findByDepartmentAndBudgetYear(
            String department,
            Integer budgetYear
    );
}