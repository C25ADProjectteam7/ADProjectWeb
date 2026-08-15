package com.expensehub.webbackend.mobile.repository;

import com.expensehub.webbackend.mobile.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    boolean existsByMobileTripId(Long mobileTripId);

    List<Approval> findByStatus(ApprovalStatus status);

    List<Approval> findByStatusInOrderByDecidedAtDesc(List<ApprovalStatus> statuses);
}