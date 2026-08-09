package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.DepartmentalBudget;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DepartmentalBudgetService {

    List<DepartmentalBudget> listBudgets(Integer year);

    DepartmentalBudget createBudget(DepartmentalBudget budget);

    DepartmentalBudget updateBudget(Long id, DepartmentalBudget budget);

    /**
     * The configured yearly cap for a department, used by the Manager approval
     * workflow (Item 20) to decide whether a trip needs approval. Empty means
     * no budget has been configured for that department this year — callers
     * should treat that as "no cap" rather than "cap of zero".
     */
    Optional<BigDecimal> getLimit(String department);
}