import apiClient from './client.js';

// TODO: 可视化分析接口占位（mobile 数据聚合结果）
export const analyticsApi = {
  getDepartmentExpenseComparison(params) {
    return apiClient.get('/analytics/department-expenses', { params });
  },
  getEmployeeTravelFrequency(params) {
    return apiClient.get('/analytics/travel-frequency', { params });
  },
  getBudgetOverrunAlerts(params) {
    return apiClient.get('/analytics/budget-alerts', { params });
  },
};
