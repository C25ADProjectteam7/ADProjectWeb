import apiClient from './client.js';

export const financeApi = {
  // budget management (Item 16)
  listBudgets(department) {
    return apiClient.get('/finance/budgets', { params: department ? { department } : {} });
  },
  upsertBudget(payload) {
    return apiClient.put('/finance/budgets', payload);
  },
  getBudgetAudit(budgetId) {
    return apiClient.get(`/finance/budgets/${budgetId}/audit`);
  },

  //Reimbursement Requests (Item 17)
  listReimbursements(params) {
    return apiClient.get('/finance/reimbursements', { params });
  },
  getReimbursement(requestId) {
    return apiClient.get(`/finance/reimbursements/${requestId}`);
  },
  reviewReimbursement(requestId, payload) {
    return apiClient.patch(`/finance/reimbursements/${requestId}/review`, payload);
  },
  getReimbursementAudit(requestId) {
    return apiClient.get(`/finance/reimbursements/${requestId}/audit`);
  },

  // Export (Item 18)
  exportReimbursements(params) {
    return apiClient.get('/finance/reimbursements/export', {
      params,
      responseType: 'blob',
    });
  },
};
