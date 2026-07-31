import apiClient from './client.js';

// TODO: 财务流程接口占位（预算、自动标记、报销审核、导出）
export const financeApi = {
  getBudgetConfig() {
    return apiClient.get('/finance/budget');
  },
  updateBudgetConfig(payload) {
    return apiClient.put('/finance/budget', payload);
  },
  listReimbursements(params) {
    return apiClient.get('/finance/reimbursements', { params });
  },
  reviewReimbursement(requestId, payload) {
    return apiClient.patch(`/finance/reimbursements/${requestId}/review`, payload);
  },
  exportReimbursements(params) {
    return apiClient.get('/finance/reimbursements/export', {
      params,
      responseType: 'blob',
    });
  },
};
