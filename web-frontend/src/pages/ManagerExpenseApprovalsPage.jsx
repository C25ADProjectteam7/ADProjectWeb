import { Fragment, useEffect, useState } from 'react';
import { managerExpenseApi } from '../api/managerExpenseApi.js';
import '../styles/theme.css';

function formatMoney(amount) {
  if (amount === undefined || amount === null) return '-';
  return `S$${Number(amount).toFixed(2)}`;
}

function formatDateTime(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString();
}

export default function ManagerExpenseApprovalsPage() {
  const [pending, setPending] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [noteDrafts, setNoteDrafts] = useState({});
  const [expandedId, setExpandedId] = useState(null);
  const [pendingPage, setPendingPage] = useState(0);
  const [pendingTotalPages, setPendingTotalPages] = useState(0);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);
  const pageSize = 10;

  useEffect(() => {
    async function loadData() {
      try {
        const [pendingRes, historyRes] = await Promise.all([
          managerExpenseApi.listPendingApprovals(pendingPage, pageSize),
          managerExpenseApi.listApprovalHistory(historyPage, pageSize),
        ]);
        setPending(pendingRes.data.content || []);
        setPendingTotalPages(pendingRes.data.totalPages || 0);
        setHistory(historyRes.data.content || []);
        setHistoryTotalPages(historyRes.data.totalPages || 0);
      } catch (err) {
        setError('Failed to load expense approval data. Please try again.');
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [pendingPage, historyPage]);

  async function handleDecision(expenseId, decision) {
    setActionError(null);
    const note = noteDrafts[expenseId] || '';
    const target = pending.find((p) => p.expenseId === expenseId);
    try {
      if (decision === 'APPROVED') {
        await managerExpenseApi.approveExpense(expenseId, { note });
      } else {
        await managerExpenseApi.rejectExpense(expenseId, { note });
      }

      setPending((prev) => prev.filter((item) => item.expenseId !== expenseId));
      setHistory((prev) => [
        {
          expenseId,
          department: target?.department,
          decision,
          decidedAt: new Date().toISOString(),
          note,
        },
        ...prev,
      ]);
    } catch (err) {
      setActionError(`Failed to submit decision for expense #${expenseId}. Please try again.`);
    }
  }

  if (loading)
    return (
      <section className="eh-page">
        <p className="eh-empty">Loading...</p>
      </section>
    );
  if (error)
    return (
      <section className="eh-page">
        <p className="eh-empty">{error}</p>
      </section>
    );

  const sortedPending = [...pending].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  return (
    <section className="eh-page">
      <div className="eh-title">Expense Approval Center</div>
      <div className="eh-subtitle">
        Review and approve expense reimbursements that exceed department budgets
      </div>

      <div className="eh-section-title">Pending Expense Approvals</div>
      {actionError && (
        <div className="eh-alert-card" style={{ marginBottom: 14 }}>
          <div>
            <div className="eh-alert-title">{actionError}</div>
          </div>
        </div>
      )}
      <div className="eh-card">
        <div className="eh-table-scroll">
          {sortedPending.length === 0 ? (
            <p className="eh-empty">No pending expense approvals right now.</p>
          ) : (
            <table className="eh-table">
              <thead>
                <tr>
                  <th>Expense #</th>
                  <th>Department</th>
                  <th>Over Budget</th>
                  <th>Submitted</th>
                  <th>Note</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {sortedPending.map((item) => (
                  <Fragment key={item.expenseId}>
                    <tr>
                      <td>
                        <span
                          style={{
                            cursor: 'pointer',
                            textDecoration: 'underline dotted',
                            textUnderlineOffset: 3,
                          }}
                          onClick={() =>
                            setExpandedId(expandedId === item.expenseId ? null : item.expenseId)
                          }
                        >
                          #{item.expenseId}
                        </span>
                      </td>
                      <td>{item.department}</td>
                      <td className="eh-mono">
                        {item.overBudgetAmount != null && Number(item.overBudgetAmount) > 0 ? (
                          <span className="eh-badge eh-badge-coral">
                            {formatMoney(item.overBudgetAmount)}
                          </span>
                        ) : (
                          <span className="eh-badge eh-badge-silver">Calculated</span>
                        )}
                      </td>
                      <td className="eh-mono" style={{ fontSize: 11.5 }}>
                        {formatDateTime(item.createdAt)}
                      </td>
                      <td>
                        <input
                          className="eh-input"
                          type="text"
                          placeholder="Note (optional)"
                          value={noteDrafts[item.expenseId] || ''}
                          onChange={(e) =>
                            setNoteDrafts((prev) => ({
                              ...prev,
                              [item.expenseId]: e.target.value,
                            }))
                          }
                        />
                      </td>
                      <td style={{ whiteSpace: 'nowrap' }}>
                        <button
                          className="eh-btn eh-btn-approve"
                          onClick={() => handleDecision(item.expenseId, 'APPROVED')}
                        >
                          Approve
                        </button>{' '}
                        <button
                          className="eh-btn eh-btn-reject"
                          onClick={() => handleDecision(item.expenseId, 'REJECTED')}
                        >
                          Reject
                        </button>
                      </td>
                    </tr>
                    {expandedId === item.expenseId && (
                      <tr>
                        <td
                          colSpan={6}
                          style={{
                            background: 'var(--runway)',
                            padding: '14px 16px',
                          }}
                        >
                          <div
                            style={{
                              fontSize: 12.5,
                              color: 'var(--ink-soft)',
                              lineHeight: 1.8,
                            }}
                          >
                            <div>
                              <b>Expense ID:</b> {item.expenseId}
                            </div>
                            <div>
                              <b>Department:</b> {item.department}
                            </div>
                            <div>
                              <b>Over-budget amount:</b> {formatMoney(item.overBudgetAmount)}
                            </div>
                            <div>
                              <b>Created:</b> {formatDateTime(item.createdAt)}
                            </div>
                            {item.mobileTripId && (
                              <div>
                                <b>Trip ID:</b> {item.mobileTripId}
                              </div>
                            )}
                            <div
                              style={{
                                marginTop: 6,
                                fontSize: 11,
                                color: 'var(--muted)',
                              }}
                            >
                              This expense exceeds the department budget and requires manager
                              approval before finance can review it.
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>
          )}
        </div>
        {pendingTotalPages > 1 && (
          <div className="eh-pagination">
            <button
              className="eh-btn"
              disabled={pendingPage === 0}
              onClick={() => setPendingPage((p) => Math.max(0, p - 1))}
            >
              ‹ Prev
            </button>
            <span className="eh-pagination-info">
              Page {pendingPage + 1} / {pendingTotalPages}
            </span>
            <button
              className="eh-btn"
              disabled={pendingPage >= pendingTotalPages - 1}
              onClick={() => setPendingPage((p) => p + 1)}
            >
              Next ›
            </button>
          </div>
        )}
      </div>

      <div className="eh-section-title">Approval History</div>
      <div className="eh-card">
        <div className="eh-table-scroll">
          {history.length === 0 ? (
            <p className="eh-empty">No expense approval history yet.</p>
          ) : (
            <table className="eh-table">
              <thead>
                <tr>
                  <th>Expense #</th>
                  <th>Department</th>
                  <th>Decision</th>
                  <th>Decided At</th>
                  <th>Note</th>
                </tr>
              </thead>
              <tbody>
                {history.map((item) => (
                  <tr key={item.expenseId || item.id}>
                    <td>#{item.expenseId}</td>
                    <td>{item.department}</td>
                    <td>
                      <span
                        className={`eh-badge ${
                          item.decision === 'APPROVED' || item.managerApproved === true
                            ? 'eh-badge-sage'
                            : 'eh-badge-coral'
                        }`}
                      >
                        {item.decision === 'APPROVED' || item.managerApproved === true
                          ? 'Approved'
                          : 'Rejected'}
                      </span>
                    </td>
                    <td className="eh-mono">
                      {formatDateTime(item.decidedAt || item.managerApprovedAt)}
                    </td>
                    <td>{item.note || item.managerNote}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        {historyTotalPages > 1 && (
          <div className="eh-pagination">
            <button
              className="eh-btn"
              disabled={historyPage === 0}
              onClick={() => setHistoryPage((p) => Math.max(0, p - 1))}
            >
              ‹ Prev
            </button>
            <span className="eh-pagination-info">
              Page {historyPage + 1} / {historyTotalPages}
            </span>
            <button
              className="eh-btn"
              disabled={historyPage >= historyTotalPages - 1}
              onClick={() => setHistoryPage((p) => p + 1)}
            >
              Next ›
            </button>
          </div>
        )}
      </div>
    </section>
  );
}
