import apiClient from './client.js';
import { mockDepartmentExpenses, mockTravelFrequency, mockBudgetAlerts } from '../mocks/analyticsMockData.js';

const USE_MOCK = true; 

function mockResponse(data) {
  return new Promise((resolve) => setTimeout(() => resolve({ data }), 300));
}

export const analyticsApi = {
  getDepartmentExpenseComparison(params) {
    if (USE_MOCK) return mockResponse(mockDepartmentExpenses);
    return apiClient.get('/analytics/department-expenses', { params });
  },
  getEmployeeTravelFrequency(params) {
    if (USE_MOCK) return mockResponse(mockTravelFrequency);
    return apiClient.get('/analytics/travel-frequency', { params });
  },
  getBudgetOverrunAlerts(params) {
    if (USE_MOCK) return mockResponse(mockBudgetAlerts);
    return apiClient.get('/analytics/budget-alerts', { params });
  },
};