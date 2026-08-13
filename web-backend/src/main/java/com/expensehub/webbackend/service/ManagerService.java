package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import com.expensehub.webbackend.integration.mobile.MobileExpenseClient;
import com.expensehub.webbackend.integration.mobile.MobileTripDTO;
import com.expensehub.webbackend.integration.mobile.MobileUserDTO;
import com.expensehub.webbackend.repository.ApprovalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Business logic for backlog Item 20: Manager Approval Notifications.
 * A trip becomes a pending approval when its requested budget exceeds its
 * department's yearly limit (see DepartmentalBudgetService). Departments with
 * no configured limit are never flagged — "not configured" is treated as
 * "no cap", not as a cap of zero.
 */
@Service
public class ManagerService {

    private final MobileExpenseClient mobileExpenseClient;
    private final ApprovalRepository approvalRepository;
    private final DepartmentalBudgetService departmentalBudgetService;

    public ManagerService(
            MobileExpenseClient mobileExpenseClient,
            ApprovalRepository approvalRepository,
            DepartmentalBudgetService departmentalBudgetService) {
        this.mobileExpenseClient = mobileExpenseClient;
        this.approvalRepository = approvalRepository;
        this.departmentalBudgetService = departmentalBudgetService;
    }

    public void syncPendingApprovalsFromMobile() {
        List<MobileTripDTO> trips = mobileExpenseClient.listAllTrips();
        for (MobileTripDTO trip : trips) {
            MobileUserDTO owner = fetchUserSafely(trip.getUserId());
            String department = (owner != null && owner.getDepartment() != null) ? owner.getDepartment() : "Unknown";
            String employeeName = owner != null ? owner.getUsername() : "Unknown";

            Optional<BigDecimal> limit = departmentalBudgetService.getLimit(department);
            boolean overBudget =
                    limit.isPresent()
                            && trip.getBudgetTotal() != null
                            && trip.getBudgetTotal().compareTo(limit.get()) > 0;
            boolean alreadyTracked = approvalRepository.existsByMobileTripId(trip.getId());
            if (overBudget && !alreadyTracked) {
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
                                .departmentBudgetLimit(limit.get())
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
        return approvalRepository.save(approval);
    }
}