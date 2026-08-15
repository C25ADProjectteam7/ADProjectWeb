import apiClient from './client.js';
import { mockPendingApprovals, mockApprovalHistory } from '../mocks/managerMockData.js';

const USE_MOCK = false;

function mockResolve(data) {
  return new Promise((resolve) => setTimeout(() => resolve({ data }), 300));
}

export const managerApi = {
  listPendingApprovals(page = 0, size = 10) {
    if (USE_MOCK) return mockResolve(mockPendingApprovals);
    return apiClient.get('/manager/approvals/pending', { params: { page, size } });
  },
  listApprovalHistory(page = 0, size = 10) {
    if (USE_MOCK) return mockResolve(mockApprovalHistory);
    return apiClient.get('/manager/approvals/history', { params: { page, size } });
  },
  approveRequest(requestId, payload) {
    if (USE_MOCK) return mockResolve({ requestId, decision: 'APPROVED', ...payload });
    return apiClient.post(`/manager/approvals/${requestId}/approve`, payload);
  },
  rejectRequest(requestId, payload) {
    if (USE_MOCK) return mockResolve({ requestId, decision: 'REJECTED', ...payload });
    return apiClient.post(`/manager/approvals/${requestId}/reject`, payload);
  },
};
