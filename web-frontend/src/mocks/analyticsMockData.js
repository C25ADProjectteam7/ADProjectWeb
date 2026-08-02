// web-frontend/src/mocks/analyticsMockData.js
export const mockDepartmentExpenses = [
  { department: 'Sales', totalExpense: 4820, budget: 3500 },
  { department: 'Engineering', totalExpense: 2100, budget: 4000 },
  { department: 'Marketing', totalExpense: 5300, budget: 3000 },
];

export const mockTravelFrequency = [
  { userId: 1, userName: 'Rachel Ong', department: 'Marketing', tripCount: 4 },
  { userId: 2, userName: 'Wei Ming Koh', department: 'Sales', tripCount: 2 },
];

export const mockBudgetAlerts = [
  { department: 'Marketing', budget: 3000, actual: 5300, overPercent: 77 },
];