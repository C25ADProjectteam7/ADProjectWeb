package com.expensehub.webbackend.repository;

import com.expensehub.webbackend.entity.ReimbursementAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementAuditLogRepository
        extends JpaRepository<ReimbursementAuditLog, Long> {

    List<ReimbursementAuditLog> findByRequestIdOrderByChangedAtDesc(Long requestId);
}
