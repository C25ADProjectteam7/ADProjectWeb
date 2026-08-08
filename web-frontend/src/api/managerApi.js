import apiClient from './client.js';

export const managerApi = {
  listPendingApprovals(params) {
    return apiClient.get('/manager/approvals/pending', { params });
  },
  listApprovalHistory(params) {
    return apiClient.get('/manager/approvals/history', { params });
  },
  approveRequest(requestId, payload) {
    return apiClient.post(`/manager/approvals/${requestId}/approve`, payload);
  },
  rejectRequest(requestId, payload) {
    return apiClient.post(`/manager/approvals/${requestId}/reject`, payload);
  },
};
