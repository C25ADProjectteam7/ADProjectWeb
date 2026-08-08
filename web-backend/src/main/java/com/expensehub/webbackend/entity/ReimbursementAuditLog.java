package com.expensehub.webbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Corresponding to backlog Item 19, the acceptance criterion is: "Edits are tracked with an audit trail (who changed what, when)."
 * At the same time, the approval action (approve/reject/request-info) for Item 17 is also recorded, facilitating the unified tracing of the complete history of a reimbursement request.
 */
@Entity
@Table(name = "reimbursement_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReimbursementAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requestId;

    /** REVIEW 或 EDIT */
    @Column(nullable = false)
    private String action;

    /** 变更详情，如 "status: SUBMITTED -> APPROVED; comment: Looks good" */
    @Column(length = 2000, nullable = false)
    private String detail;

    @Column(nullable = false)
    private String changedBy;

    @Column(nullable = false)
    private Instant changedAt;
}
