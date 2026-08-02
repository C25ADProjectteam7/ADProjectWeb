import apiClient from './client.js';
import { mockPendingApprovals, mockApprovalHistory } from '../mocks/managerMockData.js';

// mobile组的trips表暂未建好，先用本地mock数据渲染页面
// 等真实接口就绪，把 USE_MOCK 改成 false 即可，组件代码不需要改动
const USE_MOCK = true;

function mockResolve(data) {
  return new Promise((resolve) => setTimeout(() => resolve({ data }), 300));
}

export const managerApi = {
  listPendingApprovals(params) {
    if (USE_MOCK) return mockResolve(mockPendingApprovals);
    return apiClient.get('/manager/approvals/pending', { params });
  },
  listApprovalHistory(params) {
    if (USE_MOCK) return mockResolve(mockApprovalHistory);
    return apiClient.get('/manager/approvals/history', { params });
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