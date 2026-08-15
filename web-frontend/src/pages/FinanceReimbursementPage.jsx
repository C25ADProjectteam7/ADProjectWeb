import { Fragment, useCallback, useEffect, useState } from 'react';
import { financeApi } from '../api/financeApi.js';

const CATEGORIES = ['FLIGHT', 'HOTEL', 'MEAL', 'TRANSPORT', 'OTHER'];
const STATUSES = ['SUBMITTED', 'APPROVED', 'REJECTED', 'NEEDS_INFO'];
const FLAG_LABELS = {
  MISSING_RECEIPT: 'Missing Receipt',
  OVER_PER_DIEM: 'Over Per Diem',
  OVER_BUDGET: 'Over Budget',
};
const STATUS_LABELS = {
  SUBMITTED: 'Pending Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  NEEDS_INFO: 'Needs Information',
};
const PAGE_SIZE = 10;

function cleanParams(params) {
  const result = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) {
      result[key] = value;
    }
  });
  return result;
}

function formatMoney(amount, currency) {
  if (amount === undefined || amount === null) return '-';
  return `${Number(amount).toFixed(2)} ${currency || ''}`.trim();
}

function formatDateTime(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString();
}

export default function FinanceReimbursementPage() {
  //budget allocation (Item 16)
  const [budgets, setBudgets] = useState([]);
  const [budgetForm, setBudgetForm] = useState({
    department: '',
    periodType: 'QUARTERLY',
    periodLabel: '',
    amount: '',
  });
  const [budgetError, setBudgetError] = useState('');
  const [budgetSaving, setBudgetSaving] = useState(false);
  const [openBudgetAuditId, setOpenBudgetAuditId] = useState(null);
  const [budgetAuditEntries, setBudgetAuditEntries] = useState([]);

  //reimbursement requests (Item 17)
  const [filters, setFilters] = useState({
    status: '',
    department: '',
    category: '',
    from: '',
    to: '',
  });
  const [page, setPage] = useState(0);
  const [reimbursements, setReimbursements] = useState({
    content: [],
    totalPages: 0,
    totalElements: 0,
  });
  const [listError, setListError] = useState('');
  const [listLoading, setListLoading] = useState(false);

  const [expandedRowId, setExpandedRowId] = useState(null);
  const [expandedMode, setExpandedMode] = useState(null); // 'review' | 'audit'
  const [reviewForm, setReviewForm] = useState({ decision: 'APPROVE', comment: '' });
  const [rowAuditEntries, setRowAuditEntries] = useState([]);
  const [rowActionError, setRowActionError] = useState('');
  const [rowActionBusy, setRowActionBusy] = useState(false);

  const [exporting, setExporting] = useState(false);

  const loadBudgets = useCallback(async () => {
    try {
      const { data } = await financeApi.listBudgets();
      setBudgets(data);
    } catch {
      setBudgetError('Budget configuration loading failed. Please try again later.');
    }
  }, []);

  const loadReimbursements = useCallback(async () => {
    setListLoading(true);
    setListError('');
    try {
      const { data } = await financeApi.listReimbursements(
        cleanParams({ ...filters, page, size: PAGE_SIZE }),
      );
      setReimbursements(data);
    } catch {
      setListError('Reimbursement request loading failed. Please try again later.');
    } finally {
      setListLoading(false);
    }
  }, [filters, page]);

  useEffect(() => {
    loadBudgets();
  }, [loadBudgets]);

  useEffect(() => {
    loadReimbursements();
  }, [loadReimbursements]);

  // ---- 预算配置操作 ----

  async function handleBudgetSubmit(event) {
    event.preventDefault();
    setBudgetError('');
    if (
      !budgetForm.department.trim() ||
      !budgetForm.periodLabel.trim() ||
      budgetForm.amount === ''
    ) {
      setBudgetError('Department, period label, and amount are required fields.');
      return;
    }
    setBudgetSaving(true);
    try {
      await financeApi.upsertBudget({
        department: budgetForm.department.trim(),
        periodType: budgetForm.periodType,
        periodLabel: budgetForm.periodLabel.trim(),
        amount: Number(budgetForm.amount),
      });
      setBudgetForm({ department: '', periodType: 'QUARTERLY', periodLabel: '', amount: '' });
      await loadBudgets();
      await loadReimbursements();
    } catch {
      setBudgetError('Failed to save budget configuration. Please check your input and try again.');
    } finally {
      setBudgetSaving(false);
    }
  }

  async function toggleBudgetAudit(budgetId) {
    if (openBudgetAuditId === budgetId) {
      setOpenBudgetAuditId(null);
      return;
    }
    try {
      const { data } = await financeApi.getBudgetAudit(budgetId);
      setBudgetAuditEntries(data);
      setOpenBudgetAuditId(budgetId);
    } catch {
      setBudgetError('Failed to load audit trail.');
    }
  }

  function handleFilterChange(field, value) {
    setFilters((prev) => ({ ...prev, [field]: value }));
    setPage(0);
  }

  async function handleExport() {
    setExporting(true);
    try {
      const response = await financeApi.exportReimbursements(cleanParams(filters));
      const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = `reimbursements-${new Date().toISOString().slice(0, 10)}.xlsx`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(blobUrl);
    } catch {
      setListError('Export failed. Please try again later.');
    } finally {
      setExporting(false);
    }
  }

  function openReview(row) {
    setExpandedRowId(row.id);
    setExpandedMode('review');
    setReviewForm({ decision: 'APPROVE', comment: '' });
    setRowActionError('');
  }

  async function openAudit(row) {
    setExpandedRowId(row.id);
    setExpandedMode('audit');
    setRowActionError('');
    try {
      const { data } = await financeApi.getReimbursementAudit(row.id);
      setRowAuditEntries(data);
    } catch {
      setRowActionError('Failed to load audit trail.');
    }
  }

  function closeExpandedRow() {
    setExpandedRowId(null);
    setExpandedMode(null);
    setRowActionError('');
  }

  async function submitReview(requestId) {
    if (reviewForm.decision === 'REQUEST_INFO' && !reviewForm.comment.trim()) {
      setRowActionError(
        'When requesting additional information, please fill in the specific content that needs to be supplied/modified, and employees will see this comment.',
      );
      return;
    }
    setRowActionBusy(true);
    setRowActionError('');
    try {
      await financeApi.reviewReimbursement(requestId, reviewForm);
      closeExpandedRow();
      await loadReimbursements();
    } catch (err) {
      const upstreamMessage = err?.response?.data?.message;
      setRowActionError(
        upstreamMessage || 'Failed to submit review results. Please try again later.',
      );
    } finally {
      setRowActionBusy(false);
    }
  }

  // 获取状态对应的 badge 颜色类
  function getStatusBadgeClass(status) {
    switch (status) {
      case 'APPROVED':
        return 'eh-badge-sage';
      case 'REJECTED':
        return 'eh-badge-coral';
      case 'SUBMITTED':
      case 'NEEDS_INFO':
      default:
        return 'eh-badge-silver';
    }
  }

  return (
    <section className="eh-page">
      <h2 className="eh-title">Financial Reimbursement Process</h2>
      <p className="eh-subtitle">
        Budget configuration, automatic compliance marking, reimbursement request review, data
        export to Excel.
      </p>

      {/* Budget allocation*/}
      <div className="eh-card">
        <h3>Department Budget Configuration</h3>
        {budgetError && (
          <div className="eh-alert-card">
            <span className="eh-alert-title">{budgetError}</span>
          </div>
        )}

        <form className="finance-inline-form" onSubmit={handleBudgetSubmit}>
          <label>
            Department
            <input
              type="text"
              className="eh-input"
              value={budgetForm.department}
              onChange={(e) => setBudgetForm((f) => ({ ...f, department: e.target.value }))}
              placeholder="Engineering"
            />
          </label>
          <label>
            Period Type
            <select
              className="eh-input"
              value={budgetForm.periodType}
              onChange={(e) => setBudgetForm((f) => ({ ...f, periodType: e.target.value }))}
            >
              <option value="ANNUAL">Annual</option>
              <option value="QUARTERLY">Quarterly</option>
            </select>
          </label>
          <label>
            Period Label
            <input
              type="text"
              className="eh-input"
              value={budgetForm.periodLabel}
              onChange={(e) => setBudgetForm((f) => ({ ...f, periodLabel: e.target.value }))}
              placeholder={budgetForm.periodType === 'ANNUAL' ? '2026' : '2026-Q1'}
            />
          </label>
          <label>
            Budget Amount
            <input
              type="number"
              min="0"
              step="0.01"
              className="eh-input"
              value={budgetForm.amount}
              onChange={(e) => setBudgetForm((f) => ({ ...f, amount: e.target.value }))}
              placeholder="5000.00"
            />
          </label>
          <button type="submit" className="eh-btn eh-btn-approve" disabled={budgetSaving}>
            {budgetSaving ? 'Saving…' : 'Save Budget'}
          </button>
        </form>

        <div className="eh-table-scroll">
          <table className="eh-table">
            <thead>
              <tr>
                <th>Department</th>
                <th>Period</th>
                <th>Budget</th>
                <th>Spent</th>
                <th>Remaining</th>
                <th>Status</th>
                <th>Last Updated</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {budgets.length === 0 && (
                <tr>
                  <td colSpan={8} className="eh-empty">
                    No budget configuration available
                  </td>
                </tr>
              )}
              {budgets.map((b) => (
                <Fragment key={b.id}>
                  <tr>
                    <td>{b.department}</td>
                    <td>{b.periodLabel}</td>
                    <td>{formatMoney(b.amount, '')}</td>
                    <td>{formatMoney(b.spent, '')}</td>
                    <td>{formatMoney(b.remaining, '')}</td>
                    <td>
                      {b.overBudget ? (
                        <span className="eh-badge eh-badge-coral">Over Budget</span>
                      ) : (
                        <span className="eh-badge eh-badge-sage">Normal</span>
                      )}
                    </td>
                    <td>{b.updatedBy ? `${b.updatedBy} · ${formatDateTime(b.updatedAt)}` : '-'}</td>
                    <td>
                      <button type="button" className="eh-btn" onClick={() => toggleBudgetAudit(b.id)}>
                        {openBudgetAuditId === b.id ? 'Collapse Records' : 'View Changes'}
                      </button>
                    </td>
                  </tr>
                  {openBudgetAuditId === b.id && (
                    <tr>
                      <td colSpan={8}>
                        <ul className="finance-audit-list">
                          {budgetAuditEntries.length === 0 && <li>No change records available</li>}
                          {budgetAuditEntries.map((entry) => (
                            <li key={entry.id}>
                              {formatDateTime(entry.changedAt)} · {entry.changedBy} · {entry.detail}
                            </li>
                          ))}
                        </ul>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* ---------------- Reimbursement Requests / Review (Item 17) ---------------- */}
      <div className="eh-card">
        <h3>Reimbursement Requests</h3>
        <p className="eh-subtitle">
          Data comes from the Mobile app submitted by employees. When errors are found in
          amount/category information, use the &quot;Request Additional Information&quot; button
          below to reject the request, and employees will be able to modify it in the App and it
          will reappear in the pending review list — finance staff cannot directly edit the values.
        </p>

        <div className="finance-filters">
          <label>
            Status
            <select
              className="eh-input"
              value={filters.status}
              onChange={(e) => handleFilterChange('status', e.target.value)}
            >
              <option value="">All</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </label>
          <label>
            Department
            <input
              type="text"
              className="eh-input"
              value={filters.department}
              onChange={(e) => handleFilterChange('department', e.target.value)}
              placeholder="All Departments"
            />
          </label>
          <label>
            Category
            <select
              className="eh-input"
              value={filters.category}
              onChange={(e) => handleFilterChange('category', e.target.value)}
            >
              <option value="">All</option>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          <label>
            Start Date
            <input
              type="date"
              className="eh-input"
              value={filters.from}
              onChange={(e) => handleFilterChange('from', e.target.value)}
            />
          </label>
          <label>
            End Date
            <input
              type="date"
              className="eh-input"
              value={filters.to}
              onChange={(e) => handleFilterChange('to', e.target.value)}
            />
          </label>
          <button type="button" className="eh-btn" onClick={() => loadReimbursements()} disabled={listLoading}>
            {listLoading ? 'Loading…' : 'Refresh'}
          </button>
          <button type="button" className="eh-btn" onClick={handleExport} disabled={exporting}>
            {exporting ? 'Exporting…' : 'Export to Excel'}
          </button>
        </div>

        {listError && (
          <div className="eh-alert-card">
            <span className="eh-alert-title">{listError}</span>
          </div>
        )}

        <div className="eh-table-scroll">
          <table className="eh-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Department</th>
                <th>Category</th>
                <th>Amount</th>
                <th>Submitted At</th>
                <th>Receipt</th>
                <th>Status</th>
                <th>Compliance Flags</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {reimbursements.content.length === 0 && !listLoading && (
                <tr>
                  <td colSpan={9} className="eh-empty">
                    No qualifying reimbursement requests
                  </td>
                </tr>
              )}
              {reimbursements.content.map((row) => (
                <Fragment key={row.id}>
                  <tr>
                    <td>{row.employeeName}</td>
                    <td>{row.department || 'Unknown Department'}</td>
                    <td>{row.category}</td>
                    <td>{formatMoney(row.amount, row.currency)}</td>
                    <td>{formatDateTime(row.submittedAt)}</td>
                    <td>
                      {row.receiptAttached ? (
                        <a href={row.receiptUrl} target="_blank" rel="noreferrer">
                          View Receipt
                        </a>
                      ) : (
                        'No Receipt'
                      )}
                    </td>
                    <td>
                      <span className={`eh-badge ${getStatusBadgeClass(row.status)}`}>
                        {STATUS_LABELS[row.status]}
                      </span>
                    </td>
                    <td>
                      {row.policyFlags.length === 0
                        ? '-'
                        : row.policyFlags.map((flag) => (
                            <span key={flag} className="eh-badge eh-badge-coral">
                              {FLAG_LABELS[flag] || flag}
                            </span>
                          ))}
                    </td>
                    <td className="finance-row-actions">
                      <button type="button" className="eh-btn" onClick={() => openReview(row)}>
                        Review
                      </button>
                      <button type="button" className="eh-btn" onClick={() => openAudit(row)}>
                        Audit Trail
                      </button>
                    </td>
                  </tr>

                  {expandedRowId === row.id && expandedMode === 'review' && (
                    <tr>
                      <td colSpan={9}>
                        <div className="finance-expanded-panel">
                          {rowActionError && (
                            <div className="eh-alert-card">
                              <span className="eh-alert-title">{rowActionError}</span>
                            </div>
                          )}
                          <label>
                            Review Conclusion
                            <select
                              className="eh-input"
                              value={reviewForm.decision}
                              onChange={(e) =>
                                setReviewForm((f) => ({ ...f, decision: e.target.value }))
                              }
                            >
                              <option value="APPROVE">Approve</option>
                              <option value="REJECT">Reject</option>
                              <option value="REQUEST_INFO">
                                Request Additional Information / Modification
                              </option>
                            </select>
                          </label>
                          <label>
                            Comment
                            {reviewForm.decision === 'REQUEST_INFO'
                              ? ' (Required, Employee Will See)'
                              : ' (Optional)'}
                            <textarea
                              className="eh-input"
                              value={reviewForm.comment}
                              onChange={(e) =>
                                setReviewForm((f) => ({ ...f, comment: e.target.value }))
                              }
                              placeholder={
                                reviewForm.decision === 'REQUEST_INFO'
                                  ? 'For example: Amount and receipt do not match, please verify and resubmit'
                                  : 'Review comments (optional)'
                              }
                            />
                          </label>
                          <div className="finance-panel-actions">
                            <button
                              type="button"
                              className="eh-btn eh-btn-approve"
                              onClick={() => submitReview(row.id)}
                              disabled={rowActionBusy}
                            >
                              Submit Review Result
                            </button>
                            <button type="button" className="eh-btn eh-btn-reject" onClick={closeExpandedRow}>
                              Cancel
                            </button>
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}

                  {expandedRowId === row.id && expandedMode === 'audit' && (
                    <tr>
                      <td colSpan={9}>
                        <div className="finance-expanded-panel">
                          {rowActionError && (
                            <div className="eh-alert-card">
                              <span className="eh-alert-title">{rowActionError}</span>
                            </div>
                          )}
                          <ul className="finance-audit-list">
                            {rowAuditEntries.length === 0 && <li>No audit trail available</li>}
                            {rowAuditEntries.map((entry) => (
                              <li key={entry.id}>
                                {formatDateTime(entry.changedAt)} · {entry.changedBy} · [
                                {entry.action}] {entry.detail}
                              </li>
                            ))}
                          </ul>
                          <div className="finance-panel-actions">
                            <button type="button" className="eh-btn" onClick={closeExpandedRow}>
                              Collapse
                            </button>
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>

        <div className="eh-pagination">
          <button type="button" className="eh-btn" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous Page
          </button>
          <span className="eh-pagination-info">
            Page {reimbursements.totalPages === 0 ? 0 : page + 1} of {reimbursements.totalPages}
            (Total: {reimbursements.totalElements})
          </span>
          <button
            type="button"
            className="eh-btn"
            disabled={page + 1 >= reimbursements.totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next Page
          </button>
        </div>
      </div>
    </section>
  );
}