import { useEffect, useState } from 'react';
import { analyticsApi } from '../api/analyticsApi.js';
import '../styles/theme.css';

export default function AnalyticsOverviewPage() {
  const [deptExpenses, setDeptExpenses] = useState([]);
  const [travelFreq, setTravelFreq] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      const [deptRes, freqRes, alertRes] = await Promise.all([
        analyticsApi.getDepartmentExpenseComparison(),
        analyticsApi.getEmployeeTravelFrequency(),
        analyticsApi.getBudgetOverrunAlerts(),
      ]);
      setDeptExpenses(deptRes.data);
      setTravelFreq(freqRes.data);
      setAlerts(alertRes.data);
      setLoading(false);
    }
    loadData();
  }, []);

  if (loading) return <section className="eh-page"><p className="eh-empty">加载中...</p></section>;

  const totalSpend = deptExpenses.reduce((sum, d) => sum + d.totalExpense, 0);
  const totalBudget = deptExpenses.reduce((sum, d) => sum + d.budget, 0);
  const totalTrips = travelFreq.reduce((sum, t) => sum + t.tripCount, 0);
  const maxValue = Math.max(...deptExpenses.map((d) => Math.max(d.totalExpense, d.budget)), 1);

  return (
    <section className="eh-page">
      <div className="eh-title">数据分析与可视化</div>
      <div className="eh-subtitle">基于 Mobile 端数据的部门费用、出差频率和预算预警分析</div>

      <div className="eh-kpi-grid">
        <div className="eh-kpi-card">
          <div className="eh-kpi-label">总支出</div>
          <div className="eh-kpi-value">S${totalSpend.toLocaleString()}</div>
          <div className="eh-kpi-note" style={{ color: totalSpend > totalBudget ? '#A93B24' : '#4C7A6B' }}>
            {totalSpend > totalBudget ? '超出总预算' : '在总预算内'}
          </div>
        </div>
        <div className="eh-kpi-card">
          <div className="eh-kpi-label">总预算</div>
          <div className="eh-kpi-value">S${totalBudget.toLocaleString()}</div>
          <div className="eh-kpi-note" style={{ color: '#7C8698' }}>覆盖 {deptExpenses.length} 个部门</div>
        </div>
        <div className="eh-kpi-card">
          <div className="eh-kpi-label">超支部门</div>
          <div className="eh-kpi-value">{alerts.length}</div>
          <div className="eh-kpi-note" style={{ color: alerts.length > 0 ? '#A93B24' : '#4C7A6B' }}>
            {alerts.length > 0 ? '需要关注' : '暂无预警'}
          </div>
        </div>
        <div className="eh-kpi-card">
          <div className="eh-kpi-label">出差总次数</div>
          <div className="eh-kpi-value">{totalTrips}</div>
          <div className="eh-kpi-note" style={{ color: '#7C8698' }}>{travelFreq.length} 名员工</div>
        </div>
      </div>

      <div className="eh-section-title">部门费用对比</div>
      <div className="eh-card">
        {deptExpenses.map((d) => {
          const over = d.totalExpense > d.budget;
          return (
            <div className="eh-bar-row" key={d.department}>
              <div className="eh-bar-label">
                <span>{d.department}</span>
                <span className="eh-mono">S${d.totalExpense} / S${d.budget}</span>
              </div>
              <div className="eh-bar-track">
                <div
                  className="eh-bar-fill"
                  style={{
                    width: `${(d.totalExpense / maxValue) * 100}%`,
                    background: over ? 'var(--coral)' : 'var(--sage)',
                  }}
                />
              </div>
            </div>
          );
        })}
      </div>

      <div className="eh-section-title">员工出差频率统计</div>
      <div className="eh-card">
        <table className="eh-table">
          <thead><tr><th>员工</th><th>部门</th><th>出差次数</th></tr></thead>
          <tbody>
            {travelFreq.map((t) => (
              <tr key={t.userId}>
                <td>{t.userName}</td>
                <td>{t.department}</td>
                <td className="eh-mono">{t.tripCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="eh-section-title">预算超支警告</div>
      {alerts.length === 0 ? (
        <div className="eh-card"><p className="eh-empty">目前没有超支的部门。</p></div>
      ) : (
        alerts.map((a) => (
          <div className="eh-alert-card" key={a.department}>
            <div>
              <div className="eh-alert-title">{a.department} 部门超支 {a.overPercent}%</div>
              <div className="eh-alert-sub">预算 S${a.budget} · 实际 S${a.actual}</div>
            </div>
          </div>
        ))
      )}
    </section>
  );
}