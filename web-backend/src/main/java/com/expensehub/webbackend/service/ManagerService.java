package com.expensehub.webbackend.service;

import com.expensehub.webbackend.client.MobileApiClient;
import com.expensehub.webbackend.dto.MobileTripDto;
import com.expensehub.webbackend.dto.MobileUserDto;
import com.expensehub.webbackend.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import com.expensehub.webbackend.repository.ApprovalRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic for backlog Item 20: Manager Approval Notifications.
 * A trip becomes a pending approval when its requested budget exceeds its
 * department's limit (see DepartmentBudgetService).
 */
@Service
public class ManagerService {

    private final MobileApiClient mobileApiClient;
    private final ApprovalRepository approvalRepository;
    private final DepartmentBudgetService departmentBudgetService;

    public ManagerService(MobileApiClient mobileApiClient,
            ApprovalRepository approvalRepository,
            DepartmentBudgetService departmentBudgetService) {
        this.mobileApiClient = mobileApiClient;
        this.approvalRepository = approvalRepository;
        this.departmentBudgetService = departmentBudgetService;
    }

    public void syncPendingApprovalsFromMobile() {
        List<MobileTripDto> trips = mobileApiClient.fetchAllTrips();
        for (MobileTripDto trip : trips) {
            MobileUserDto owner = fetchUserSafely(trip.userId());
            String department = (owner != null && owner.department() != null) ? owner.department() : "Unknown";
            String employeeName = owner != null ? owner.username() : "Unknown";

            BigDecimal limit = departmentBudgetService.getLimit(department);
            boolean overBudget = trip.budgetTotal() != null && trip.budgetTotal().compareTo(limit) > 0;
            boolean alreadyTracked = approvalRepository.existsByMobileTripId(trip.id());
            if (overBudget && !alreadyTracked) {
                Approval approval = Approval.builder()
                        .mobileTripId(trip.id())
                        .tripTitle(trip.title())
                        .employeeName(employeeName)
                        .department(department)
                        .destination(trip.destination())
                        .startDate(LocalDate.parse(trip.startDate()))
                        .endDate(LocalDate.parse(trip.endDate()))
                        .budgetRequested(trip.budgetTotal())
                        .departmentBudgetLimit(limit)
                        .status(ApprovalStatus.PENDING)
                        .submittedAt(trip.createdAt() != null
                                ? LocalDateTime.parse(trip.createdAt())
                                : LocalDateTime.now())
                        .build();
                approvalRepository.save(approval);
            }
        }
    }

    private MobileUserDto fetchUserSafely(Long userId) {
        try {
            return mobileApiClient.fetchUser(userId);
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