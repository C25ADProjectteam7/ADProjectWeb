package com.expensehub.webbackend.service;

import com.expensehub.webbackend.client.MobileApiClient;
import com.expensehub.webbackend.dto.MobileTripDto;
import com.expensehub.webbackend.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import com.expensehub.webbackend.repository.ApprovalRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic for backlog Item 20: Manager Approval Notifications.
 * A trip becomes a pending approval when its requested budget exceeds the
 * department's limit.
 * TODO: department budget limits are hardcoded below; replace with a lookup
 * against the Department Budget Configuration feature (backlog Item 16).
 * TODO: managerId is not set on approve/reject yet — wire it up once this
 * controller requires an authenticated MANAGER (see SecurityConfig / JWT).
 */
@Service
public class ManagerService {

    private final MobileApiClient mobileApiClient;
    private final ApprovalRepository approvalRepository;

    private static final BigDecimal DEFAULT_DEPARTMENT_LIMIT = BigDecimal.valueOf(1500);

    public ManagerService(MobileApiClient mobileApiClient, ApprovalRepository approvalRepository) {
        this.mobileApiClient = mobileApiClient;
        this.approvalRepository = approvalRepository;
    }

    public void syncPendingApprovalsFromMobile() {
        List<MobileTripDto> trips = mobileApiClient.fetchAllTrips();
        for (MobileTripDto trip : trips) {
            boolean overBudget = trip.budgetTotal() != null
                    && trip.budgetTotal().compareTo(DEFAULT_DEPARTMENT_LIMIT) > 0;
            boolean alreadyTracked = approvalRepository.existsByMobileTripId(trip.id());
            if (overBudget && !alreadyTracked) {
                Approval approval = Approval.builder()
                        .mobileTripId(trip.id())
                        .employeeName(trip.employeeName())
                        .department(trip.department())
                        .destination(trip.destination())
                        .startDate(LocalDate.parse(trip.startDate()))
                        .endDate(LocalDate.parse(trip.endDate()))
                        .budgetRequested(trip.budgetTotal())
                        .departmentBudgetLimit(DEFAULT_DEPARTMENT_LIMIT)
                        .status(ApprovalStatus.PENDING)
                        .submittedAt(LocalDateTime.now())
                        .build();
                approvalRepository.save(approval);
            }
        }
    }

    public List<Approval> listPending() {
        syncPendingApprovalsFromMobile();
        return approvalRepository.findByStatus(ApprovalStatus.PENDING);
    }

    public List<Approval> listHistory() {
        return approvalRepository.findByStatusInOrderByDecidedAtDesc(
                List.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED));
    }

    public Approval decide(Long approvalId, ApprovalStatus decision, String note) {
        Approval approval = approvalRepository.findById(approvalId).orElseThrow();
        approval.setStatus(decision);
        approval.setNote(note);
        approval.setDecidedAt(LocalDateTime.now());
        return approvalRepository.save(approval);
    }
}