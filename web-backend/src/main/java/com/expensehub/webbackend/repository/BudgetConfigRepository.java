package com.expensehub.webbackend.repository;

import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.BudgetPeriodType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetConfigRepository extends JpaRepository<BudgetConfig, Long> {

    Optional<BudgetConfig> findByDepartmentAndPeriodTypeAndPeriodLabel(
            String department, BudgetPeriodType periodType, String periodLabel);

    List<BudgetConfig> findByDepartment(String department);

    List<BudgetConfig> findAllByOrderByDepartmentAscPeriodLabelAsc();
    
    Optional<BudgetConfig> findTopByDepartmentOrderByPeriodLabelDesc(String department);
}
