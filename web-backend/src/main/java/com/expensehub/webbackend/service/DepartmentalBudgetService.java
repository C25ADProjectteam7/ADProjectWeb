package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.DepartmentalBudget;

import java.util.List;

public interface DepartmentalBudgetService {

    List<DepartmentalBudget> listBudgets(Integer year);

    DepartmentalBudget createBudget(DepartmentalBudget budget);

    DepartmentalBudget updateBudget(Long id, DepartmentalBudget budget);
}