package com.expensehub.webbackend.mobile.repository;

import com.expensehub.webbackend.mobile.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    boolean existsByMobileTripId(Long mobileTripId);

    java.util.Optional<Approval> findByMobileTripId(Long mobileTripId);

    Page<Approval> findByStatus(ApprovalStatus status, Pageable pageable);

    Page<Approval> findByStatusInOrderByDecidedAtDesc(List<ApprovalStatus> statuses, Pageable pageable);
}