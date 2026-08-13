export const mockDepartmentExpensesByPeriod = {
  this_month: [
    { department: 'Sales', totalExpense: 4820, budget: 3500 },
    { department: 'Engineering', totalExpense: 3450, budget: 4000 },
    { department: 'Marketing', totalExpense: 5300, budget: 3000 },
  ],
  last_month: [
    { department: 'Sales', totalExpense: 3100, budget: 3500 },
    { department: 'Engineering', totalExpense: 3800, budget: 4000 },
    { department: 'Marketing', totalExpense: 2950, budget: 3000 },
  ],
  this_quarter: [
    { department: 'Sales', totalExpense: 12400, budget: 10500 },
    { department: 'Engineering', totalExpense: 9600, budget: 12000 },
    { department: 'Marketing', totalExpense: 14200, budget: 9000 },
  ],
};

export const mockTravelFrequencyByPeriod = {
  this_month: [
    { userId: 1, userName: 'Rachel Ong', department: 'Marketing', tripCount: 4 },
    { userId: 2, userName: 'Wei Ming Koh', department: 'Sales', tripCount: 2 },
    { userId: 3, userName: 'Daniel Lim', department: 'Sales', tripCount: 3 },
    { userId: 4, userName: 'Ashley Tan', department: 'Engineering', tripCount: 1 },
  ],
  last_month: [
    { userId: 1, userName: 'Rachel Ong', department: 'Marketing', tripCount: 2 },
    { userId: 2, userName: 'Wei Ming Koh', department: 'Sales', tripCount: 3 },
    { userId: 3, userName: 'Daniel Lim', department: 'Sales', tripCount: 1 },
    { userId: 4, userName: 'Ashley Tan', department: 'Engineering', tripCount: 2 },
  ],
  this_quarter: [
    { userId: 1, userName: 'Rachel Ong', department: 'Marketing', tripCount: 9 },
    { userId: 2, userName: 'Wei Ming Koh', department: 'Sales', tripCount: 7 },
    { userId: 3, userName: 'Daniel Lim', department: 'Sales', tripCount: 5 },
    { userId: 4, userName: 'Ashley Tan', department: 'Engineering', tripCount: 4 },
  ],
};

export const mockAlertTransactions = {
  Marketing: [
    { id: 1, employeeName: 'Rachel Ong', category: 'Hotel', amount: 1800, date: '2026-07-30' },
    { id: 2, employeeName: 'Rachel Ong', category: 'Flight', amount: 2100, date: '2026-07-28' },
    { id: 3, employeeName: 'Daniel Lim', category: 'Meal', amount: 1400, date: '2026-07-25' },
  ],
  Sales: [
    { id: 4, employeeName: 'Wei Ming Koh', category: 'Flight', amount: 3200, date: '2026-07-29' },
    { id: 5, employeeName: 'Wei Ming Koh', category: 'Hotel', amount: 1620, date: '2026-07-27' },
  ],
  Engineering: [
    { id: 6, employeeName: 'Ashley Tan', category: 'Hotel', amount: 2100, date: '2026-07-26' },
    { id: 7, employeeName: 'Ashley Tan', category: 'Flight', amount: 1350, date: '2026-07-24' },
  ],
};

export const mockExpenseCategories = [
  { category: 'FLIGHT', amount: 9800 },
  { category: 'HOTEL', amount: 6400 },
  { category: 'MEAL', amount: 2100 },
  { category: 'TRANSPORT', amount: 1200 },
  { category: 'OTHER', amount: 720 },
];

export const mockMonthlyTrend = [
  { month: '2026-02', amount: 8200 },
  { month: '2026-03', amount: 9100 },
  { month: '2026-04', amount: 7600 },
  { month: '2026-05', amount: 10300 },
  { month: '2026-06', amount: 9800 },
  { month: '2026-07', amount: 12200 },
];

export const mockApprovalOutcomes = {
  approved: 14,
  rejected: 5,
  pending: 3,
  avgTurnaroundHours: 18.4,
};