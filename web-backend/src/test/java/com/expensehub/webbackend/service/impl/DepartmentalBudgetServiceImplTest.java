package com.expensehub.webbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expensehub.webbackend.entity.DepartmentalBudget;
import com.expensehub.webbackend.repository.DepartmentalBudgetRepository;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentalBudgetServiceImplTest {

    @Mock private DepartmentalBudgetRepository budgetRepository;

    private DepartmentalBudgetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DepartmentalBudgetServiceImpl(budgetRepository);
    }

    private DepartmentalBudget budget(String department, int year, String amount) {
        DepartmentalBudget b = new DepartmentalBudget();
        b.setDepartment(department);
        b.setBudgetYear(year);
        b.setBudgetAmount(new BigDecimal(amount));
        return b;
    }

    @Test
    void listBudgets_withYear_filtersByYear() {
        when(budgetRepository.findByBudgetYear(2026))
                .thenReturn(List.of(budget("Sales", 2026, "3500")));

        List<DepartmentalBudget> result = service.listBudgets(2026);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartment()).isEqualTo("Sales");
        verify(budgetRepository, never()).findAll();
    }

    @Test
    void listBudgets_withoutYear_returnsAll() {
        when(budgetRepository.findAll())
                .thenReturn(List.of(budget("Sales", 2026, "3500"), budget("Engineering", 2025, "4000")));

        List<DepartmentalBudget> result = service.listBudgets(null);

        assertThat(result).hasSize(2);
        verify(budgetRepository, never()).findByBudgetYear(any());
    }

    @Test
    void createBudget_noExistingEntry_savesSuccessfully() {
        DepartmentalBudget toCreate = budget("Marketing", 2026, "3000");
        when(budgetRepository.findByDepartmentAndBudgetYear("Marketing", 2026))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(toCreate)).thenReturn(toCreate);

        DepartmentalBudget result = service.createBudget(toCreate);

        assertThat(result.getDepartment()).isEqualTo("Marketing");
        verify(budgetRepository, times(1)).save(toCreate);
    }

    @Test
    void createBudget_duplicateDepartmentAndYear_throws() {
        DepartmentalBudget toCreate = budget("Marketing", 2026, "3000");
        when(budgetRepository.findByDepartmentAndBudgetYear("Marketing", 2026))
                .thenReturn(Optional.of(budget("Marketing", 2026, "2500")));

        assertThatThrownBy(() -> service.createBudget(toCreate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void updateBudget_existingId_updatesFields() {
        DepartmentalBudget existing = budget("Sales", 2026, "3500");
        existing.setId(1L);
        DepartmentalBudget updates = budget("Sales", 2026, "5000");

        when(budgetRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(existing)).thenReturn(existing);

        DepartmentalBudget result = service.updateBudget(1L, updates);

        assertThat(result.getBudgetAmount()).isEqualByComparingTo("5000");
    }

    @Test
    void updateBudget_missingId_throws() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBudget(99L, budget("Sales", 2026, "5000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getLimit_configuredForCurrentYear_returnsAmount() {
        int thisYear = Year.now().getValue();
        when(budgetRepository.findByDepartmentAndBudgetYear("Sales", thisYear))
                .thenReturn(Optional.of(budget("Sales", thisYear, "3500")));

        Optional<BigDecimal> result = service.getLimit("Sales");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("3500");
    }

    @Test
    void getLimit_notConfigured_returnsEmpty() {
        int thisYear = Year.now().getValue();
        when(budgetRepository.findByDepartmentAndBudgetYear(eq("Unknown"), eq(thisYear)))
                .thenReturn(Optional.empty());

        Optional<BigDecimal> result = service.getLimit("Unknown");

        assertThat(result).isEmpty();
    }
}
