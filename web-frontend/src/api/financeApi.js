import apiClient from './client.js';

export const financeApi = {
  listBudgets(params) {
    return apiClient.get('/finance/budgets', { params });
  },

  createBudget(payload) {
    return apiClient.post('/finance/budgets', payload);
  },

  updateBudget(id, payload) {
    return apiClient.put(`/finance/budgets/${id}`, payload);
  },

  listReimbursements() {
    return apiClient.get('/finance/reimbursements');
  },

  createReimbursement(payload) {
    return apiClient.post('/finance/reimbursements', payload);
  },

  updateReimbursement(id, payload) {
    return apiClient.put(`/finance/reimbursements/${id}`, payload);
  },

  approveReimbursement(id) {
    return apiClient.patch(`/finance/reimbursements/${id}/approve`);
  },

  rejectReimbursement(id) {
    return apiClient.patch(`/finance/reimbursements/${id}/reject`);
  },
};
