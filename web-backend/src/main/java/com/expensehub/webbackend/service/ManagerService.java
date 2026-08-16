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
                // A trip the employee cancelled before it ever reached the
                // approval queue needs no approval record at all.
                if ("CANCELLED".equalsIgnoreCase(trip.getStatus())) {
                    continue;
                }
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
            } else {
                Optional<BigDecimal> limit = budgetLookupService.resolveBudgetLimit(department);
                // Sync: the employee cancelled the trip after it became
                // pending - mark the approval decided (REJECTED with an
                // explanatory note) so it leaves the manager's pending list.
                Optional<Approval> existing = approvalRepository.findByMobileTripId(trip.getId());
                existing.filter(a -> a.getStatus() == ApprovalStatus.PENDING
                        && "CANCELLED".equalsIgnoreCase(trip.getStatus()))
                        .ifPresent(a -> {
                            a.setStatus(ApprovalStatus.REJECTED);
                            a.setNote("Trip was cancelled by the employee");
                            a.setDecidedAt(LocalDateTime.now());
                            approvalRepository.save(a);
                        });
                existing.filter(a -> a.getStatus() == ApprovalStatus.PENDING
                        && !"CANCELLED".equalsIgnoreCase(trip.getStatus()))
                        .ifPresent(a -> {
                            BigDecimal latestLimit = limit.orElse(null);
                            if (!sameMoney(a.getDepartmentBudgetLimit(), latestLimit)) {
                                a.setDepartmentBudgetLimit(latestLimit);
                                approvalRepository.save(a);
                            }
                        });
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

    private boolean sameMoney(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    @Scheduled(fixedRate = 60000)
    public void autoSyncPendingApprovals() {
        syncPendingApprovalsFromMobile();
    }

    public org.springframework.data.domain.Page<Approval> listPending(int page, int size) {
        syncPendingApprovalsFromMobile();
        return approvalRepository.findByStatus(
                ApprovalStatus.PENDING,
                org.springframework.data.domain.PageRequest.of(
                        page, size,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "submittedAt")));
    }

    public org.springframework.data.domain.Page<Approval> listHistory(int page, int size) {
        return approvalRepository.findByStatusInOrderByDecidedAtDesc(
                List.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED),
                org.springframework.data.domain.PageRequest.of(page, size));
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
        // Mobile's trips.status enum now includes REJECTED (it was added
        // after the initial integration) - a rejected approval maps directly,
        // and the Mobile app distinguishes it from the employee's own
        // CANCELLED trips.
        mobileJdbcTemplate.update(
                "UPDATE trips SET status = ? WHERE id = ?",
                decision.name(),
                approval.getMobileTripId());
        return approval;
    }
}
