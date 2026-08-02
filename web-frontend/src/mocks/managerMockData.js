export const mockPendingApprovals = [
  {
    requestId: 101,
    employeeName: 'Rachel Ong',
    department: 'Marketing',
    destination: 'Jakarta',
    startDate: '2026-08-12',
    endDate: '2026-08-14',
    budgetRequested: 2400,
    departmentBudgetLimit: 2000,
    overBudgetPercent: 20,
    submittedAt: '2026-07-28T09:12:00Z',
  },
  {
    requestId: 102,
    employeeName: 'Ashley Tan',
    department: 'Engineering',
    destination: 'Bangkok',
    startDate: '2026-08-20',
    endDate: '2026-08-22',
    budgetRequested: 1800,
    departmentBudgetLimit: 1500,
    overBudgetPercent: 20,
    submittedAt: '2026-07-30T11:05:00Z',
  },
];

export const mockApprovalHistory = [
  {
    requestId: 98,
    employeeName: 'Wei Ming Koh',
    destination: 'Bangkok',
    decision: 'APPROVED',
    decidedAt: '2026-07-25T14:00:00Z',
    note: 'Client meeting justified the extra cost.',
  },
  {
    requestId: 95,
    employeeName: 'Rachel Ong',
    destination: 'Seoul',
    decision: 'REJECTED',
    decidedAt: '2026-07-20T10:30:00Z',
    note: 'Over budget with no clear justification.',
  },
];