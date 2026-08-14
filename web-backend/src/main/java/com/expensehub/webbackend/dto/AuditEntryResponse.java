package com.expensehub.webbackend.dto;

import java.time.Instant;

/** General audit entry DTO, used for budget change records in Item 16 and reimbursement modification records in Item 19.  */
public record AuditEntryResponse(
        Long id, String action, String detail, String changedBy, Instant changedAt) {}
