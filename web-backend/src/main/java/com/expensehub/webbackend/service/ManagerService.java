package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.ApprovalStatus;
import com.expensehub.webbackend.integration.mobile.MobileExpenseClient;
import com.expensehub.webbackend.integration.mobile.MobileTripDTO;
import com.expensehub.webbackend.integration.mobile.MobileUserDTO;
import com.expensehub.webbackend.mobile.entity.Approval;
import com.expensehub.webbackend.mobile.repository.ApprovalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Business logic for backlog Item 20: Manager Approval Notifications.
 * Every trip submitted via Mobile becomes a pending approval — managers
 * review all of them, not just ones over budget (per team decision:
 * "manager must see all trip requests, normal ones need approval too").
 * departmentBudgetLimit is carried along as reference information only,
 * not as a filter for whether a trip appears here.
 * <p>
 * The budget figure itself is read from Finance's BudgetConfig (Item 16 —
 * the same quarterly/annual department budgets finance staff configure),
 * not a separate manager-only budget table: quarter takes priority over
 * annual when both are configured for the same department.
 */
@Service
public class ManagerService {

    private final MobileExpenseClient mobileExpenseClient;
    private final ApprovalRepository approvalRepository;
    private final BudgetLookupService budgetLookupService;
    // Raw access to Mobile's trips table - the approval status flip below
    // (decide) writes the same database the approvals table lives in now.
    private final JdbcTemplate mobileJdbcTemplate;

    public ManagerService(
            MobileExpenseClient mobileExpenseClient,
            ApprovalRepository approvalRepository,
            BudgetLookupService budgetLookupService,
            @Qualifier("mobileJdbcTemplate") JdbcTemplate mobileJdbcTemplate) {
        this.mobileExpenseClient = mobileExpenseClient;
        this.approvalRepository = approvalRepository;
        this.budgetLookupService = budgetLookupService;
        this.mobileJdbcTemplate = mobileJdbcTemplate;
    }

    public void syncPendingApprovalsFromMobile() {
        List<MobileTripDTO> trips = mobileExpenseClient.listAllTrips();
        for (MobileTripDTO trip : trips) {
            MobileUserDTO owner = fetchUserSafely(trip.getUserId());
            String department = (owner != null && owner.getDepartment() != null) ? owner.getDepartment() : "Unknown";
            String employeeName = owner != null ? owner.getUsername() : "Unknown";

            boolean alreadyTracked = approvalRepository.existsByMobileTripId(trip.getId());
            if (!alreadyTracked) {
                Optional<BigDecimal> limit = budgetLookupService.resolveBudgetLimit(department);
                Approval approval =
                        Approval.builder()
                                .mobileTripId(trip.getId())
                                .tripTitle(trip.getTitle())
                                .employeeName(employeeName)
                                .department(department)
                                .destination(trip.getDestination())
                                .startDate(LocalDate.parse(trip.getStartDate()))
                                .endDate(LocalDate.parse(trip.getEndDate()))
                                .budgetRequested(trip.getBudgetTotal())
                                .departmentBudgetLimit(limit.orElse(null))
                                .status(ApprovalStatus.PENDING)
                                .submittedAt(
                                        trip.getCreatedAt() != null
                                                ? LocalDateTime.parse(trip.getCreatedAt())
                                                : LocalDateTime.now())
                                .build();
                approvalRepository.save(approval);
            }
        }
    }

    private MobileUserDTO fetchUserSafely(Long userId) {
        try {
            return mobileExpenseClient.getUser(userId);
        } catch (Exception e) {
            return null;
        }
    }

    @Scheduled(fixedRate = 60000)
    public void autoSyncPendingApprovals() {
        syncPendingApprovalsFromMobile();
    }

    public List<Approval> listPending() {
        syncPendingApprovalsFromMobile();
        return approvalRepository.findByStatus(ApprovalStatus.PENDING);
    }

    public List<Approval> listHistory() {
        return approvalRepository.findByStatusInOrderByDecidedAtDesc(
                List.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED));
    }

    public Approval decide(Long approvalId, ApprovalStatus decision, String note, Long managerId) {
        Approval approval = approvalRepository.findById(approvalId).orElseThrow();
        approval.setStatus(decision);
        approval.setNote(note);
        approval.setManagerId(managerId);
        approval.setDecidedAt(LocalDateTime.now());
        approval = approvalRepository.save(approval);

        // Keep the Mobile app in sync: the approvals table now lives in
        // Mobile's database, so flip the trip's status in the same database.
        // Mobile's trips.status enum has no REJECTED value (observed live:
        // writing it 500'd with a MySQL enum error) - a rejected approval maps
        // to CANCELLED, which the Mobile app renders as the trip being
        // disapproved/cancelled.
        String mobileStatus = decision == ApprovalStatus.REJECTED ? "CANCELLED" : decision.name();
        mobileJdbcTemplate.update(
                "UPDATE trips SET status = ? WHERE id = ?",
                mobileStatus,
                approval.getMobileTripId());
        return approval;
    }
}
