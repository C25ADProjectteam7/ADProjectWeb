package com.expensehub.webbackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.BudgetPeriodType;
import com.expensehub.webbackend.entity.ReimbursementCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReimbursementPolicyEngineTest {

    private final ReimbursementPolicyEngine engine = new ReimbursementPolicyEngine();

    @Test
    void flagsMissingReceipt() {
        List<String> flags =
                engine.evaluate(
                        new BigDecimal("20.00"),
                        ReimbursementCategory.MEAL,
                        false,
                        Optional.empty(),
                        new BigDecimal("20.00"));

        assertThat(flags).contains(ReimbursementPolicyEngine.FLAG_MISSING_RECEIPT);
    }

    @Test
    void doesNotFlagReceiptWhenAttached() {
        List<String> flags =
                engine.evaluate(
                        new BigDecimal("20.00"),
                        ReimbursementCategory.MEAL,
                        true,
                        Optional.empty(),
                        new BigDecimal("20.00"));

        assertThat(flags).doesNotContain(ReimbursementPolicyEngine.FLAG_MISSING_RECEIPT);
    }

    @Test
    void flagsAmountOverPerDiemLimit() {
        List<String> flags =
                engine.evaluate(
                        new BigDecimal("500.00"),
                        ReimbursementCategory.MEAL,
                        true,
                        Optional.empty(),
                        new BigDecimal("500.00"));

        assertThat(flags).contains(ReimbursementPolicyEngine.FLAG_OVER_PER_DIEM);
    }

    @Test
    void flagsOverBudgetWhenPeriodSpendExceedsConfiguredAmount() {
        BudgetConfig budget =
                BudgetConfig.builder()
                        .department("Engineering")
                        .periodType(BudgetPeriodType.QUARTERLY)
                        .periodLabel("2026-Q1")
                        .amount(new BigDecimal("1000.00"))
                        .build();

        List<String> flags =
                engine.evaluate(
                        new BigDecimal("50.00"),
                        ReimbursementCategory.TRANSPORT,
                        true,
                        Optional.of(budget),
                        new BigDecimal("1050.00"));

        assertThat(flags).contains(ReimbursementPolicyEngine.FLAG_OVER_BUDGET);
    }

    @Test
    void doesNotFlagOverBudgetWhenWithinLimit() {
        BudgetConfig budget =
                BudgetConfig.builder()
                        .department("Engineering")
                        .periodType(BudgetPeriodType.QUARTERLY)
                        .periodLabel("2026-Q1")
                        .amount(new BigDecimal("1000.00"))
                        .build();

        List<String> flags =
                engine.evaluate(
                        new BigDecimal("50.00"),
                        ReimbursementCategory.TRANSPORT,
                        true,
                        Optional.of(budget),
                        new BigDecimal("900.00"));

        assertThat(flags).doesNotContain(ReimbursementPolicyEngine.FLAG_OVER_BUDGET);
    }

    @Test
    void cleanClaimHasNoFlags() {
        List<String> flags =
                engine.evaluate(
                        new BigDecimal("30.00"),
                        ReimbursementCategory.MEAL,
                        true,
                        Optional.empty(),
                        new BigDecimal("30.00"));

        assertThat(flags).isEmpty();
    }
}
