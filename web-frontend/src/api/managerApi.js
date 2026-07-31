import apiClient from './client.js';

// TODO: 经理审批接口占位（待办、历史、批准、驳回、备注）
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
