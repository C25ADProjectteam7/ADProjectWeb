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
  const [noteDrafts, setNoteDrafts] = useState({});

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
        setError('加载审批数据失败，请稍后重试');
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  async function handleDecision(requestId, decision) {
    const note = noteDrafts[requestId] || '';
    const target = pending.find((p) => p.requestId === requestId);
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
  }

  if (loading)
    return (
      <section className="eh-page">
        <p className="eh-empty">加载中...</p>
      </section>
    );
  if (error)
    return (
      <section className="eh-page">
        <p className="eh-empty">{error}</p>
      </section>
    );

  const overdueCount = pending.filter((item) => isOverdue(item.submittedAt)).length;
  const sortedPending = [...pending].sort((a, b) => {
    const aOverdue = isOverdue(a.submittedAt);
    const bOverdue = isOverdue(b.submittedAt);
    if (aOverdue !== bOverdue) return aOverdue ? -1 : 1;
    return new Date(a.submittedAt) - new Date(b.submittedAt);
  });

  return (
    <section className="eh-page">
      <div className="eh-title">经理审批中心</div>
      <div className="eh-subtitle">员工提交预订申请后的通知、批准 / 驳回与备注</div>

      <div className="eh-section-title">待办审批列表</div>
      {overdueCount > 0 && (
        <div className="eh-alert-card">
          <div>
            <div className="eh-alert-title">{overdueCount} 笔申请已超过 48 小时未处理</div>
            <div className="eh-alert-sub">建议优先处理下方标记为“已超时”的申请</div>
          </div>
        </div>
      )}
      <div className="eh-card">
        {sortedPending.length === 0 ? (
          <p className="eh-empty">暂无待处理的审批申请。</p>
        ) : (
          <table className="eh-table">
            <thead>
              <tr>
                <th>员工</th>
                <th>部门</th>
                <th>目的地</th>
                <th>日期</th>
                <th>申请预算</th>
                <th>部门限额</th>
                <th>超支情况</th>
                <th>备注</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {sortedPending.map((item) => (
                <tr key={item.requestId}>
                  <td>{item.employeeName}</td>
                  <td>{item.department}</td>
                  <td>{item.destination}</td>
                  <td className="eh-mono">
                    {item.startDate} ~ {item.endDate}
                    {isOverdue(item.submittedAt) && (
                      <div>
                        <span className="eh-badge eh-badge-coral" style={{ marginTop: 4 }}>
                          已超时
                        </span>
                      </div>
                    )}
                  </td>
                  <td className="eh-mono">S${item.budgetRequested}</td>
                  <td className="eh-mono">S${item.departmentBudgetLimit}</td>
                  <td>
                    <span
                      className={`eh-badge ${item.overBudgetPercent > 0 ? 'eh-badge-coral' : 'eh-badge-sage'}`}
                    >
                      {item.overBudgetPercent > 0 ? `超支 ${item.overBudgetPercent}%` : '预算内'}
                    </span>
                  </td>
                  <td>
                    <input
                      className="eh-input"
                      type="text"
                      placeholder="备注（可选）"
                      value={noteDrafts[item.requestId] || ''}
                      onChange={(e) =>
                        setNoteDrafts((prev) => ({ ...prev, [item.requestId]: e.target.value }))
                      }
                    />
                  </td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <button
                      className="eh-btn eh-btn-approve"
                      onClick={() => handleDecision(item.requestId, 'APPROVED')}
                    >
                      批准
                    </button>{' '}
                    <button
                      className="eh-btn eh-btn-reject"
                      onClick={() => handleDecision(item.requestId, 'REJECTED')}
                    >
                      驳回
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="eh-section-title">审批历史记录</div>
      <div className="eh-card">
        <table className="eh-table">
          <thead>
            <tr>
              <th>员工</th>
              <th>目的地</th>
              <th>结果</th>
              <th>处理时间</th>
              <th>备注</th>
            </tr>
          </thead>
          <tbody>
            {history.map((item) => (
              <tr key={item.requestId}>
                <td>{item.employeeName}</td>
                <td>{item.destination}</td>
                <td>
                  <span
                    className={`eh-badge ${item.decision === 'APPROVED' ? 'eh-badge-sage' : 'eh-badge-coral'}`}
                  >
                    {item.decision === 'APPROVED' ? '已批准' : '已驳回'}
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
