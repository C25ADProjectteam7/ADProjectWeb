import { useEffect, useState } from 'react';
import { managerApi } from '../api/managerApi.js';
import '../styles/theme.css';

function isOverdue(submittedAt) {
  const hoursSinceSubmit = (Date.now() - new Date(submittedAt).getTime()) / (1000 * 60 * 60);
  return hoursSinceSubmit >= 48;
}

function overBudgetPercent(item) {
  const requested = Number(item.budgetRequested);
  const limit = Number(item.departmentBudgetLimit);
  const hasLimit = limit > 0;
  if (hasLimit === false || requested <= limit) return 0;
  return Math.round(((requested - limit) / limit) * 100);
}

export default function ManagerApprovalsPage() {
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
  const PAGE_SIZE = 10;

  useEffect(() => {
    async function loadData() {
      try {
        const [pendingRes, historyRes] = await Promise.all([
          managerApi.listPendingApprovals(pendingPage, PAGE_SIZE),
          managerApi.listApprovalHistory(historyPage, PAGE_SIZE),
        ]);
        setPending(pendingRes.data.content || []);
        setPendingTotalPages(pendingRes.data.totalPages || 0);
        setHistory(historyRes.data.content || []);
        setHistoryTotalPages(historyRes.data.totalPages || 0);
      } catch (err) {
        setError('Failed to load approval data. Please try again.');
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [pendingPage, historyPage]);

  async function handleDecision(id, decision) {
    setActionError(null);
    const note = noteDrafts[id] || '';
    const target = pending.find((p) => p.id === id);
    try {
      const action =
        decision === 'APPROVED'
          ? managerApi.approveRequest(id, { note })
          : managerApi.rejectRequest(id, { note });
      const res = await action;

      setPending((prev) => prev.filter((item) => item.id !== id));
      setHistory((prev) => [
        {
          id,
          employeeName: target?.employeeName,
          destination: target?.destination,
          decision: res.data.status,
          decidedAt: new Date().toISOString(),
          note,
        },
        ...prev,
      ]);
    } catch (err) {
      setActionError(
        `Failed to submit decision for ${target?.employeeName ?? 'this request'}. Please try again.`,
      );
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

  // A "REJECTED" approval whose note is the auto-sync cancellation marker
  // (ManagerService) is an employee cancellation, not a manager rejection -
  // render it distinctly.
  const isCancelledByEmployee = (item) => (item.note || '').includes('cancelled by the employee');

  const overdueCount = pending.filter((item) => isOverdue(item.submittedAt)).length;
  // Newest submissions first (the backend already sorts by submittedAt desc,
  // this is a safety net for any client-side reordering).
  const sortedPending = [...pending].sort(
    (a, b) => new Date(b.submittedAt) - new Date(a.submittedAt),
  );

  return (
    <section className="eh-page">
      <div className="eh-title">Manager Approval Center</div>
      <div className="eh-subtitle">
        Notifications, approve / reject and notes for employee trip requests
      </div>

      <div className="eh-section-title">Pending Approvals</div>
      {overdueCount > 0 && (
        <div className="eh-alert-card">
          <div>
            <div className="eh-alert-title">
              {overdueCount} request(s) overdue by more than 48 hours
            </div>
            <div className="eh-alert-sub">
              Requests marked &quot;Overdue&quot; below should be prioritized
            </div>
          </div>
        </div>
      )}
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
            <p className="eh-empty">No pending approvals right now.</p>
          ) : (
            <table className="eh-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Department</th>
                  <th>Destination</th>
                  <th>Dates</th>
                  <th>Submitted</th>
                  <th>Requested</th>
                  <th>Dept. Limit</th>
                  <th>Status</th>
                  <th>Note</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {sortedPending.map((item) => (
                  <>
                    <tr key={item.id}>
                      <td>
                        <span
                          style={{
                            cursor: 'pointer',
                            textDecoration: 'underline dotted',
                            textUnderlineOffset: 3,
                          }}
                          onClick={() => setExpandedId(expandedId === item.id ? null : item.id)}
                        >
                          {item.employeeName}
                        </span>
                      </td>
                      <td>{item.department}</td>
                      <td>{item.destination}</td>
                      <td className="eh-mono">
                        {item.startDate} ~ {item.endDate}
                      </td>
                      <td className="eh-mono" style={{ fontSize: 11.5 }}>
                        {new Date(item.submittedAt).toLocaleString()}
                        {isOverdue(item.submittedAt) && (
                          <div>
                            <span className="eh-badge eh-badge-coral" style={{ marginTop: 4 }}>
                              Overdue
                            </span>
                          </div>
                        )}
                      </td>
                      <td className="eh-mono">S${item.budgetRequested}</td>
                      <td className="eh-mono">S${item.departmentBudgetLimit}</td>
                      <td>
                        <span
                          className={`eh-badge ${overBudgetPercent(item) > 0 ? 'eh-badge-coral' : 'eh-badge-sage'}`}
                        >
                          {overBudgetPercent(item) > 0
                            ? `+${overBudgetPercent(item)}% over`
                            : 'Within budget'}
                        </span>
                      </td>
                      <td>
                        <input
                          className="eh-input"
                          type="text"
                          placeholder="Note (optional)"
                          value={noteDrafts[item.id] || ''}
                          onChange={(e) =>
                            setNoteDrafts((prev) => ({ ...prev, [item.id]: e.target.value }))
                          }
                        />
                      </td>
                      <td style={{ whiteSpace: 'nowrap' }}>
                        <button
                          className="eh-btn eh-btn-approve"
                          onClick={() => handleDecision(item.id, 'APPROVED')}
                        >
                          Approve
                        </button>{' '}
                        <button
                          className="eh-btn eh-btn-reject"
                          onClick={() => handleDecision(item.id, 'REJECTED')}
                        >
                          Reject
                        </button>
                      </td>
                    </tr>
                    {expandedId === item.id && (
                      <tr>
                        <td
                          colSpan={10}
                          style={{ background: 'var(--runway)', padding: '14px 16px' }}
                        >
                          <div
                            style={{ fontSize: 12.5, color: 'var(--ink-soft)', lineHeight: 1.8 }}
                          >
                            <div>
                              <b>Trip purpose:</b> {item.tripTitle || '(not provided)'}
                            </div>
                            <div>
                              <b>Submitted:</b> {new Date(item.submittedAt).toLocaleString()}
                            </div>
                            <div style={{ marginTop: 6, fontSize: 11, color: 'var(--muted)' }}>
                              Receipts and reimbursement details are reviewed after the trip in the
                              Reimbursement Review module (Task 3). This stage only approves the
                              pre-trip budget request.
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </>
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
          <table className="eh-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Destination</th>
                <th>Decision</th>
                <th>Decided At</th>
                <th>Note</th>
              </tr>
            </thead>
            <tbody>
              {history.map((item) => (
                <tr key={item.id}>
                  <td>{item.employeeName}</td>
                  <td>{item.destination}</td>
                  <td>
                    <span
                      className={`eh-badge ${
                        item.decision === 'APPROVED' || item.status === 'APPROVED'
                          ? 'eh-badge-sage'
                          : isCancelledByEmployee(item)
                            ? 'eh-badge-silver'
                            : 'eh-badge-coral'
                      }`}
                    >
                      {item.decision === 'APPROVED' || item.status === 'APPROVED'
                        ? 'Approved'
                        : isCancelledByEmployee(item)
                          ? 'Cancelled by employee'
                          : 'Rejected'}
                    </span>
                  </td>
                  <td className="eh-mono">{new Date(item.decidedAt).toLocaleString()}</td>
                  <td>{item.note}</td>
                </tr>
              ))}
            </tbody>
          </table>
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
