package com.expensehub.webbackend.dto;

import jakarta.validation.constraints.NotNull;

/** For Backlog Item 17: The reviewer can approve, reject, or request additional information, and provide comments. */
public record ReimbursementReviewRequest(@NotNull ReviewDecision decision, String comment) {

    public enum ReviewDecision {
        APPROVE,
        REJECT,
        REQUEST_INFO
    }
}
