import apiClient from './client.js';
import {
  mockDepartmentExpensesByPeriod,
  mockTravelFrequencyByPeriod,
  mockAlertTransactions,
  mockExpenseCategories,
  mockMonthlyTrend,
  mockApprovalOutcomes,
} from '../mocks/analyticsMockData.js';

const USE_MOCK = true;

function mockResolve(data) {
  return new Promise((resolve) => setTimeout(() => resolve({ data }), 300));
}

function computeAlerts(period) {
  const rows = mockDepartmentExpensesByPeriod[period] || [];
  return rows
    .filter((r) => r.totalExpense / r.budget >= 0.85)
    .map((r) => {
      const overPercent = Math.round(((r.totalExpense - r.budget) / r.budget) * 100);
      return {
        department: r.department,
        budget: r.budget,
        actual: r.totalExpense,
        overPercent,
        level: r.totalExpense > r.budget ? 'OVER' : 'NEAR',
      };
    });
}

export const analyticsApi = {
  getDepartmentExpenseComparison(params = { period: 'this_month' }) {
    if (USE_MOCK) return mockResolve(mockDepartmentExpensesByPeriod[params.period] || []);
    return apiClient.get('/analytics/department-expenses', { params });
  },
  getEmployeeTravelFrequency(params = { period: 'this_month' }) {
    if (USE_MOCK) return mockResolve(mockTravelFrequencyByPeriod[params.period] || []);
    return apiClient.get('/analytics/travel-frequency', { params });
  },
  getBudgetOverrunAlerts(params = { period: 'this_month' }) {
    if (USE_MOCK) return mockResolve(computeAlerts(params.period));
    return apiClient.get('/analytics/budget-alerts', { params });
  },
  getAlertTransactions(department) {
    if (USE_MOCK) return mockResolve(mockAlertTransactions[department] || []);
    return apiClient.get(`/analytics/budget-alerts/${department}/transactions`);
  },
  getExpenseCategoryBreakdown() {
    if (USE_MOCK) return mockResolve(mockExpenseCategories);
    return apiClient.get('/analytics/expense-categories');
  },
  getMonthlySpendTrend() {
    if (USE_MOCK) return mockResolve(mockMonthlyTrend);
    return apiClient.get('/analytics/monthly-trend');
  },
  getApprovalOutcomes() {
    if (USE_MOCK) return mockResolve(mockApprovalOutcomes);
    return apiClient.get('/analytics/approval-outcomes');
  },
};