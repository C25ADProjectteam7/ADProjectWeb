package com.expensehub.webbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.BudgetPeriodType;
import com.expensehub.webbackend.repository.BudgetConfigRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetLookupServiceTest {

    @Mock private BudgetConfigRepository budgetConfigRepository;

    private BudgetLookupService service;

    private static final LocalDate TODAY = LocalDate.now();
    private static final String THIS_QUARTER_LABEL = BudgetPeriodResolver.quarterlyLabel(TODAY);
    private static final String THIS_YEAR_LABEL = BudgetPeriodResolver.annualLabel(TODAY);

    private BudgetConfig config(BudgetPeriodType type, String label, String amount) {
        return BudgetConfig.builder()
                .department("Sales")
                .periodType(type)
                .periodLabel(label)
                .amount(new BigDecimal(amount))
                .build();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new BudgetLookupService(budgetConfigRepository);
    }

    @Test
    void quarterlyConfigured_returnsQuarterlyAmount() {
        when(budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                "Sales", BudgetPeriodType.QUARTERLY, THIS_QUARTER_LABEL))
                .thenReturn(Optional.of(config(BudgetPeriodType.QUARTERLY, THIS_QUARTER_LABEL, "1500")));

        Optional<BigDecimal> result = service.resolveBudgetLimit("Sales");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("1500");
    }

    @Test
    void onlyAnnualConfigured_fallsBackToAnnual() {
        when(budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                "Sales", BudgetPeriodType.QUARTERLY, THIS_QUARTER_LABEL))
                .thenReturn(Optional.empty());
        when(budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                "Sales", BudgetPeriodType.ANNUAL, THIS_YEAR_LABEL))
                .thenReturn(Optional.of(config(BudgetPeriodType.ANNUAL, THIS_YEAR_LABEL, "5000")));

        Optional<BigDecimal> result = service.resolveBudgetLimit("Sales");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("5000");
    }

    @Test
    void bothConfigured_quarterlyTakesPriority() {
        when(budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                "Sales", BudgetPeriodType.QUARTERLY, THIS_QUARTER_LABEL))
                .thenReturn(Optional.of(config(BudgetPeriodType.QUARTERLY, THIS_QUARTER_LABEL, "1500")));

        Optional<BigDecimal> result = service.resolveBudgetLimit("Sales");

        assertThat(result.get()).isEqualByComparingTo("1500");
    }

    @Test
    void neitherConfigured_returnsEmpty() {
        when(budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                "Sales", BudgetPeriodType.QUARTERLY, THIS_QUARTER_LABEL))
                .thenReturn(Optional.empty());
        when(budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                "Sales", BudgetPeriodType.ANNUAL, THIS_YEAR_LABEL))
                .thenReturn(Optional.empty());

        Optional<BigDecimal> result = service.resolveBudgetLimit("Sales");

        assertThat(result).isEmpty();
    }
}
