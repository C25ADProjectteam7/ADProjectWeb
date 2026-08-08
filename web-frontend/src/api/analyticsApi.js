import apiClient from './client.js';

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
