package com.expensehub.webbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expensehub.webbackend.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import com.expensehub.webbackend.integration.mobile.MobileExpenseClient;
import com.expensehub.webbackend.integration.mobile.MobileTripDTO;
import com.expensehub.webbackend.integration.mobile.MobileUserDTO;
import com.expensehub.webbackend.repository.ApprovalRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock private MobileExpenseClient mobileExpenseClient;
    @Mock private ApprovalRepository approvalRepository;
    @Mock private BudgetLookupService budgetLookupService;

    private ManagerService service;

    @BeforeEach
    void setUp() {
        service = new ManagerService(mobileExpenseClient, approvalRepository, budgetLookupService);
    }

    private MobileTripDTO trip(Long id, Long userId, String budget) {
        MobileTripDTO t = new MobileTripDTO();
        t.setId(id);
        t.setUserId(userId);
        t.setTitle("Client visit");
        t.setDestination("Tokyo");
        t.setStartDate("2026-09-01");
        t.setEndDate("2026-09-05");
        t.setBudgetTotal(new BigDecimal(budget));
        t.setStatus("PENDING");
        t.setCreatedAt("2026-08-20T10:00:00");
        return t;
    }

    private MobileUserDTO user(String department) {
        MobileUserDTO u = new MobileUserDTO();
        u.setUsername("rachel");
        u.setDepartment(department);
        return u;
    }

    @Test
    void sync_everyTripNotYetTracked_createsApproval_regardlessOfBudget() {
        // Team decision: manager sees ALL trip requests, not just over-budget
        // ones — this trip is well within its (generous) budget and must
        // still show up.
        when(mobileExpenseClient.listAllTrips()).thenReturn(List.of(trip(1L, 10L, "500")));
        when(mobileExpenseClient.getUser(10L)).thenReturn(user("Sales"));
        when(budgetLookupService.resolveBudgetLimit("Sales")).thenReturn(Optional.of(new BigDecimal("100000")));
        when(approvalRepository.existsByMobileTripId(1L)).thenReturn(false);

        service.syncPendingApprovalsFromMobile();

        ArgumentCaptor<Approval> captor = ArgumentCaptor.forClass(Approval.class);
        verify(approvalRepository, times(1)).save(captor.capture());
        Approval saved = captor.getValue();
        assertThat(saved.getMobileTripId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(saved.getDepartmentBudgetLimit()).isEqualByComparingTo("100000");
    }

    @Test
    void sync_noBudgetConfigured_stillCreatesApproval_withNullLimit() {
        when(mobileExpenseClient.listAllTrips()).thenReturn(List.of(trip(1L, 10L, "500")));
        when(mobileExpenseClient.getUser(10L)).thenReturn(user("R&D"));
        when(budgetLookupService.resolveBudgetLimit("R&D")).thenReturn(Optional.empty());
        when(approvalRepository.existsByMobileTripId(1L)).thenReturn(false);

        service.syncPendingApprovalsFromMobile();

        ArgumentCaptor<Approval> captor = ArgumentCaptor.forClass(Approval.class);
        verify(approvalRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getDepartmentBudgetLimit()).isNull();
    }

    @Test
    void sync_alreadyTracked_doesNotCreateDuplicate() {
        when(mobileExpenseClient.listAllTrips()).thenReturn(List.of(trip(1L, 10L, "500")));
        when(approvalRepository.existsByMobileTripId(1L)).thenReturn(true);

        service.syncPendingApprovalsFromMobile();

        verify(approvalRepository, never()).save(any());
        verify(budgetLookupService, never()).resolveBudgetLimit(any());
    }

    @Test
    void sync_mobileUserLookupFails_fallsBackToUnknownDepartment_stillCreatesApproval() {
        when(mobileExpenseClient.listAllTrips()).thenReturn(List.of(trip(1L, 10L, "500")));
        when(mobileExpenseClient.getUser(10L)).thenThrow(new RuntimeException("Mobile API unreachable"));
        when(budgetLookupService.resolveBudgetLimit("Unknown")).thenReturn(Optional.empty());
        when(approvalRepository.existsByMobileTripId(1L)).thenReturn(false);

        service.syncPendingApprovalsFromMobile();

        ArgumentCaptor<Approval> captor = ArgumentCaptor.forClass(Approval.class);
        verify(approvalRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getDepartment()).isEqualTo("Unknown");
        assertThat(captor.getValue().getEmployeeName()).isEqualTo("Unknown");
    }

    @Test
    void listPending_syncsFirstThenReturnsPendingOnly() {
        when(mobileExpenseClient.listAllTrips()).thenReturn(List.of());
        when(approvalRepository.findByStatus(ApprovalStatus.PENDING))
                .thenReturn(List.of(Approval.builder().status(ApprovalStatus.PENDING).build()));

        List<Approval> result = service.listPending();

        assertThat(result).hasSize(1);
        verify(mobileExpenseClient, times(1)).listAllTrips();
    }

    @Test
    void listHistory_returnsApprovedAndRejectedOnly() {
        when(approvalRepository.findByStatusInOrderByDecidedAtDesc(
                List.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED)))
                .thenReturn(List.of(Approval.builder().status(ApprovalStatus.APPROVED).build()));

        List<Approval> result = service.listHistory();

        assertThat(result).hasSize(1);
    }

    @Test
    void decide_approved_setsStatusNoteManagerIdAndDecidedAt() {
        Approval existing = Approval.builder().id(5L).status(ApprovalStatus.PENDING).build();
        when(approvalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(approvalRepository.save(existing)).thenReturn(existing);

        Approval result = service.decide(5L, ApprovalStatus.APPROVED, "Looks good", 42L);

        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(result.getNote()).isEqualTo("Looks good");
        assertThat(result.getManagerId()).isEqualTo(42L);
        assertThat(result.getDecidedAt()).isNotNull();
    }
}
