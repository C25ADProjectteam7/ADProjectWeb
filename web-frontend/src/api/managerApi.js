import apiClient from './client.js';
import { mockPendingApprovals, mockApprovalHistory } from '../mocks/managerMockData.js';

const USE_MOCK = true;

function mockResolve(data) {
  return new Promise((resolve) => setTimeout(() => resolve({ data }), 300));
}

export const managerApi = {
  listPendingApprovals() {
    if (USE_MOCK) return mockResolve(mockPendingApprovals);
    return apiClient.get('/manager/approvals/pending');
  },
  listApprovalHistory() {
    if (USE_MOCK) return mockResolve(mockApprovalHistory);
    return apiClient.get('/manager/approvals/history');
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