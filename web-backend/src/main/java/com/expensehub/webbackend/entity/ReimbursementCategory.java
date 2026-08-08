package com.expensehub.webbackend.entity;

/**
 * Reimbursement expense category. Directly adopt the enumeration values of Expense.ExpenseCategory in the Mobile group
 * (FLIGHT/HOTEL/MEAL/TRANSPORT/OTHER), to avoid making another name conversion mistake on both sides.
 */
public enum ReimbursementCategory {
    FLIGHT,
    HOTEL,
    MEAL,
    TRANSPORT,
    OTHER
}
