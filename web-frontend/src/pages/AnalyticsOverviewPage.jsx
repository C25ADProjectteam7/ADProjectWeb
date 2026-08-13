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

  if (loading) return <section className="eh-page"><p className="eh-empty">Loading...</p></section>;

  const totalSpend = deptExpenses.reduce((sum, d) => sum + d.totalExpense, 0);
  const totalBudget = deptExpenses.reduce((sum, d) => sum + d.budget, 0);
  const totalTrips = travelFreq.reduce((sum, t) => sum + t.tripCount, 0);
  const maxValue = Math.max(...deptExpenses.map((d) => Math.max(d.totalExpense, d.budget)), 1);

  return (
    <section className="eh-page">
      <div className="eh-title">Analytics & Visualization</div>
      <div className="eh-subtitle">Department expenses, travel frequency, and budget alerts based on Mobile app data</div>

      <div className="eh-kpi-grid">
        <div className="eh-kpi-card">
          <div className="eh-kpi-label">Total Spend</div>
          <div className="eh-kpi-value">S${totalSpend.toLocaleString()}</div>
          <div className="eh-kpi-note" style={{ color: totalSpend > totalBudget ? '#A93B24' : '#4C7A6B' }}>
            {totalSpend > totalBudget ? 'Over total budget' : 'Within total budget'}
          </div>
        </div>
        <div className="eh-kpi-card">
          <div className="eh-kpi-label">Total Budget</div>
          <div className="eh-kpi-value">S${totalBudget.toLocaleString()}</div>
          <div className="eh-kpi-note" style={{ color: '#7C8698' }}>Covers {deptExpenses.length} department{deptExpenses.length === 1 ? '' : 's'}</div>
        </div>
        <div className="eh-kpi-card">
          <div className="eh-kpi-label">Over-Budget Departments</div>
          <div className="eh-kpi-value">{alerts.length}</div>
          <div className="eh-kpi-note" style={{ color: alerts.length > 0 ? '#A93B24' : '#4C7A6B' }}>
            {alerts.length > 0 ? 'Needs attention' : 'No alerts'}
          </div>
        </div>
        <div className="eh-kpi-card">
          <div className="eh-kpi-label">Total Trips</div>
          <div className="eh-kpi-value">{totalTrips}</div>
          <div className="eh-kpi-note" style={{ color: '#7C8698' }}>{travelFreq.length} employee{travelFreq.length === 1 ? '' : 's'}</div>
        </div>
      </div>

      <div className="eh-section-title">Department Expense Comparison</div>
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

      <div className="eh-section-title">Employee Travel Frequency</div>
      <div className="eh-card">
        <table className="eh-table">
          <thead><tr><th>Employee</th><th>Department</th><th>Trips</th></tr></thead>
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

      <div className="eh-section-title">Budget Overrun Alerts</div>
      {alerts.length === 0 ? (
        <div className="eh-card"><p className="eh-empty">No departments are currently over budget.</p></div>
      ) : (
        alerts.map((a) => (
          <div className="eh-alert-card" key={a.department}>
            <div>
              <div className="eh-alert-title">{a.department} is over budget by {a.overPercent}%</div>
              <div className="eh-alert-sub">Budget S${a.budget} · Actual S${a.actual}</div>
            </div>
          </div>
        ))
      )}
    </section>
  );
}
