import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import FinanceReimbursementPage from '../src/pages/FinanceReimbursementPage.jsx';
import { financeApi } from '../src/api/financeApi.js';

vi.mock('../src/api/financeApi.js', () => ({
  financeApi: {
    listBudgets: vi.fn(),
    upsertBudget: vi.fn(),
    getBudgetAudit: vi.fn(),
    listReimbursements: vi.fn(),
    getReimbursement: vi.fn(),
    reviewReimbursement: vi.fn(),
    getReimbursementAudit: vi.fn(),
    exportReimbursements: vi.fn(),
  },
}));

describe('FinanceReimbursementPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    financeApi.listBudgets.mockResolvedValue({
      data: [
        {
          id: 1,
          department: 'Engineering',
          periodType: 'QUARTERLY',
          periodLabel: '2026-Q1',
          amount: 5000,
          spent: 620,
          remaining: 4380,
          overBudget: false,
          updatedBy: 'finance@expensehub.com',
          updatedAt: '2026-01-15T00:00:00Z',
        },
      ],
    });
    financeApi.listReimbursements.mockResolvedValue({
      data: {
        content: [
          {
            id: 10,
            employeeName: 'alice',
            employeeUserId: 1,
            department: 'Engineering',
            category: 'MEAL',
            amount: 42.5,
            currency: 'CNY',
            description: 'Client dinner',
            receiptAttached: false,
            receiptUrl: null,
            status: 'SUBMITTED',
            submittedAt: '2026-02-10T09:00:00',
            policyFlags: ['MISSING_RECEIPT'],
            reviewComment: null,
            reviewedBy: null,
          },
        ],
        totalPages: 1,
        totalElements: 1,
      },
    });
  });

  it('renders budget configuration and reimbursement list from the API', async () => {
    render(
      <MemoryRouter initialEntries={['/finance/reimbursements']}>
        <FinanceReimbursementPage />
      </MemoryRouter>,
    );

    expect(screen.getByText('Finance Reimbursement Process')).toBeInTheDocument();

    await waitFor(() => expect(financeApi.listBudgets).toHaveBeenCalled());
    await waitFor(() => expect(financeApi.listReimbursements).toHaveBeenCalled());

    expect((await screen.findAllByText('Engineering')).length).toBeGreaterThan(0);
    expect(await screen.findByText('alice')).toBeInTheDocument();
    expect(await screen.findByText('MISSING_RECEIPT')).toBeInTheDocument();
    expect(await screen.findByText('No Receipt')).toBeInTheDocument();
  });
});
