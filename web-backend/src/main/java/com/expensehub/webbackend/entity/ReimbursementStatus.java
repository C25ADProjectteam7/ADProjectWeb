package com.expensehub.webbackend.entity;

/**
 * Reimbursement request status. Directly adopt the enumeration values of Expense.ExpenseStatus in the Mobile group
 * (SUBMITTED/APPROVED/REJECTED/NEEDS_INFO). The authoritative source of data is always the Mobile terminal.
 */
public enum ReimbursementStatus {
    SUBMITTED,
    APPROVED,
    REJECTED,
    NEEDS_INFO
}
