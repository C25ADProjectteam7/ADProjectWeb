import { useEffect, useState } from 'react';
import { managerApi } from '../api/managerApi.js';
import '../styles/theme.css';

function isOverdue(submittedAt) {
  const hoursSinceSubmit = (Date.now() - new Date(submittedAt).getTime()) / (1000 * 60 * 60);
  return hoursSinceSubmit >= 48;
}

export default function ManagerApprovalsPage() {
  const [pending, setPending] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [noteDrafts, setNoteDrafts] = useState({});
  const [expandedId, setExpandedId] = useState(null);

  useEffect(() => {
    async function loadData() {
      try {
        const [pendingRes, historyRes] = await Promise.all([
          managerApi.listPendingApprovals(),
          managerApi.listApprovalHistory(),
        ]);
        setPending(pendingRes.data);
        setHistory(historyRes.data);
      } catch (err) {
        setError('Failed to load approval data. Please try again.');
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  async function handleDecision(requestId, decision) {
    setActionError(null);
    const note = noteDrafts[requestId] || '';
    const target = pending.find((p) => p.requestId === requestId);
    try {
      const action =
        decision === 'APPROVED'
          ? managerApi.approveRequest(requestId, { note })
          : managerApi.rejectRequest(requestId, { note });
      const res = await action;

      setPending((prev) => prev.filter((item) => item.requestId !== requestId));
      setHistory((prev) => [
        {
          requestId,
          employeeName: target?.employeeName,
          destination: target?.destination,
          decision: res.data.decision,
          decidedAt: new Date().toISOString(),
          note,
        },
        ...prev,
      ]);
    } catch (err) {
      setActionError(`Failed to submit decision for ${target?.employeeName ?? 'this request'}. Please try again.`);
    }
  }

  if (loading) return <section className="eh-page"><p className="eh-empty">Loading...</p></section>;
  if (error) return <section className="eh-page"><p className="eh-empty">{error}</p></section>;

  const overdueCount = pending.filter((item) => isOverdue(item.submittedAt)).length;
  const sortedPending = [...pending].sort((a, b) => {
    const aOverdue = isOverdue(a.submittedAt);
    const bOverdue = isOverdue(b.submittedAt);
    if (aOverdue !== bOverdue) return aOverdue ? -1 : 1;
    return new Date(a.submittedAt) - new Date(b.submittedAt);
  });

  return (
    <section className="eh-page">
      <div className="eh-title">Manager Approval Center</div>
      <div className="eh-subtitle">Notifications, approve / reject and notes for employee trip requests</div>

      <div className="eh-section-title">Pending Approvals</div>
      {overdueCount > 0 && (
        <div className="eh-alert-card">
          <div>
            <div className="eh-alert-title">{overdueCount} request(s) overdue by more than 48 hours</div>
            <div className="eh-alert-sub">Requests marked "Overdue" below should be prioritized</div>
          </div>
        </div>
      )}
      {actionError && (
        <div className="eh-alert-card" style={{ marginBottom: 14 }}>
          <div><div className="eh-alert-title">{actionError}</div></div>
        </div>
      )}
      <div className="eh-card">
        {sortedPending.length === 0 ? (
          <p className="eh-empty">No pending approvals right now.</p>
        ) : (
          <table className="eh-table">
            <thead>
              <tr>
                <th>Employee</th><th>Department</th><th>Destination</th><th>Dates</th>
                <th>Submitted</th>
                <th>Requested</th><th>Dept. Limit</th><th>Status</th>
                <th>Note</th><th>Action</th>
              </tr>
            </thead>
            <tbody>
              {sortedPending.map((item) => (
                <>
                  <tr key={item.requestId}>
                    <td>
                      <span
                        style={{ cursor: 'pointer', textDecoration: 'underline dotted', textUnderlineOffset: 3 }}
                        onClick={() => setExpandedId(expandedId === item.requestId ? null : item.requestId)}
                      >
                        {item.employeeName}
                      </span>
                    </td>
                    <td>{item.department}</td>
                    <td>{item.destination}</td>
                    <td className="eh-mono">{item.startDate} ~ {item.endDate}</td>
                    <td className="eh-mono" style={{ fontSize: 11.5 }}>
                      {new Date(item.submittedAt).toLocaleString()}
                      {isOverdue(item.submittedAt) && (
                        <div><span className="eh-badge eh-badge-coral" style={{ marginTop: 4 }}>Overdue</span></div>
                      )}
                    </td>
                    <td className="eh-mono">S${item.budgetRequested}</td>
                    <td className="eh-mono">S${item.departmentBudgetLimit}</td>
                    <td>
                      <span className={`eh-badge ${item.overBudgetPercent > 0 ? 'eh-badge-coral' : 'eh-badge-sage'}`}>
                        {item.overBudgetPercent > 0 ? `+${item.overBudgetPercent}% over` : 'Within budget'}
                      </span>
                    </td>
                    <td>
                      <input
                        className="eh-input"
                        type="text"
                        placeholder="Note (optional)"
                        value={noteDrafts[item.requestId] || ''}
                        onChange={(e) =>
                          setNoteDrafts((prev) => ({ ...prev, [item.requestId]: e.target.value }))
                        }
                      />
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button className="eh-btn eh-btn-approve" onClick={() => handleDecision(item.requestId, 'APPROVED')}>Approve</button>{' '}
                      <button className="eh-btn eh-btn-reject" onClick={() => handleDecision(item.requestId, 'REJECTED')}>Reject</button>
                    </td>
                  </tr>
                  {expandedId === item.requestId && (
                    <tr>
                      <td colSpan={10} style={{ background: 'var(--runway)', padding: '14px 16px' }}>
                        <div style={{ fontSize: 12.5, color: 'var(--ink-soft)', lineHeight: 1.8 }}>
                          <div><b>Trip purpose:</b> {item.tripTitle || '(not provided)'}</div>
                          <div><b>Submitted:</b> {new Date(item.submittedAt).toLocaleString()}</div>
                          <div style={{ marginTop: 6, fontSize: 11, color: 'var(--muted)' }}>
                            Receipts and reimbursement details are reviewed after the trip in the Reimbursement Review module (Task 3). This stage only approves the pre-trip budget request.
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

      <div className="eh-section-title">Approval History</div>
      <div className="eh-card">
        <table className="eh-table">
          <thead>
            <tr><th>Employee</th><th>Destination</th><th>Decision</th><th>Decided At</th><th>Note</th></tr>
          </thead>
          <tbody>
            {history.map((item) => (
              <tr key={item.requestId}>
                <td>{item.employeeName}</td>
                <td>{item.destination}</td>
                <td>
                  <span className={`eh-badge ${item.decision === 'APPROVED' ? 'eh-badge-sage' : 'eh-badge-coral'}`}>
                    {item.decision === 'APPROVED' ? 'Approved' : 'Rejected'}
                  </span>
                </td>
                <td className="eh-mono">{new Date(item.decidedAt).toLocaleString()}</td>
                <td>{item.note}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}