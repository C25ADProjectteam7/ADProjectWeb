package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.ReimbursementCategory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;


@Component
public class ReimbursementPolicyEngine {

    public static final String FLAG_MISSING_RECEIPT = "MISSING_RECEIPT";
    public static final String FLAG_OVER_PER_DIEM = "OVER_PER_DIEM";
    public static final String FLAG_OVER_BUDGET = "OVER_BUDGET";


    private static final Map<ReimbursementCategory, BigDecimal> PER_DIEM_LIMITS =
            Map.of(
                    ReimbursementCategory.MEAL, new BigDecimal("75.00"),
                    ReimbursementCategory.HOTEL, new BigDecimal("300.00"),
                    ReimbursementCategory.TRANSPORT, new BigDecimal("100.00"),
                    ReimbursementCategory.FLIGHT, new BigDecimal("1500.00"),
                    ReimbursementCategory.OTHER, new BigDecimal("150.00"));


    public List<String> evaluate(
            BigDecimal amount,
            ReimbursementCategory category,
            boolean receiptAttached,
            Optional<BudgetConfig> budget,
            BigDecimal periodSpentIncludingThis) {
        List<String> flags = new ArrayList<>();

        if (!receiptAttached) {
            flags.add(FLAG_MISSING_RECEIPT);
        }

        BigDecimal perDiemLimit = PER_DIEM_LIMITS.get(category);
        if (perDiemLimit != null && amount.compareTo(perDiemLimit) > 0) {
            flags.add(FLAG_OVER_PER_DIEM);
        }

        if (budget.isPresent()
                && periodSpentIncludingThis.compareTo(budget.get().getAmount()) > 0) {
            flags.add(FLAG_OVER_BUDGET);
        }

        return flags;
    }
}
