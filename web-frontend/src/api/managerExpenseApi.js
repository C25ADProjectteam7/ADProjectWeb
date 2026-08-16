import apiClient from './client.js';

/**
 * API client for manager expense approvals.
 * Separate from the existing managerApi which handles trip approvals.
 */
export const managerExpenseApi = {
  /**
   * List pending expense approvals needing manager decision.
   * @param {number} page - Page number (0-indexed)
   * @param {number} size - Page size
   * @param {string} department - Optional department filter
   * @returns {Promise} Promise with pending expense approvals
   */
  listPendingApprovals(page = 0, size = 10, department = null) {
    const params = { page, size };
    if (department) {
      params.department = department;
    }
    return apiClient.get('/manager/expense-approvals/pending', { params });
  },

  /**
   * List expense approval history (decided by managers).
   * @param {number} page - Page number (0-indexed)
   * @param {number} size - Page size
   * @returns {Promise} Promise with approval history
   */
  listApprovalHistory(page = 0, size = 10) {
    return apiClient.get('/manager/expense-approvals/history', {
      params: { page, size },
    });
  },

  /**
   * Approve an expense as a manager.
   * @param {number} expenseId - Expense ID
   * @param {Object} payload - Approval payload
   * @param {string} payload.note - Optional note
   * @returns {Promise} Promise with approval result
   */
  approveExpense(expenseId, payload = {}) {
    return apiClient.post(`/manager/expense-approvals/${expenseId}/approve`, payload);
  },

  /**
   * Reject an expense as a manager.
   * @param {number} expenseId - Expense ID
   * @param {Object} payload - Rejection payload
   * @param {string} payload.note - Optional note
   * @returns {Promise} Promise with rejection result
   */
  rejectExpense(expenseId, payload = {}) {
    return apiClient.post(`/manager/expense-approvals/${expenseId}/reject`, payload);
  },

  /**
   * Check if manager has decided on an expense.
   * @param {number} expenseId - Expense ID
   * @returns {Promise} Promise with decision info
   */
  getDecision(expenseId) {
    return apiClient.get(`/manager/expense-approvals/${expenseId}/decision`);
  },
};
