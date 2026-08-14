import { useEffect, useRef, useState } from 'react';
import { analyticsApi } from '../api/analyticsApi.js';
import DepartmentBudgetChart from '../charts/DepartmentBudgetChart.jsx';
import TravelRankingChart from '../charts/TravelRankingChart.jsx';
import CategoryBreakdownChart from '../charts/CategoryBreakdownChart.jsx';
import MonthlyTrendChart from '../charts/MonthlyTrendChart.jsx';
import ApprovalOutcomeChart from '../charts/ApprovalOutcomeChart.jsx';
import '../styles/theme.css';

function downloadChart(ref, filename) {
  if (!ref.current) return;
  const url = ref.current.toBase64Image();
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
}

function ChartPanel({ title, chartRef, filename, height = 220, children }) {
  return (
    <div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'baseline',
          marginBottom: '8px',
        }}
      >
        <span className="eh-section-title" style={{ margin: 0 }}>{title}</span>
        <button
          onClick={() => downloadChart(chartRef, filename)}
          style={{
            fontSize: '12px',
            color: '#3A4767',
            textDecoration: 'underline',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: 0,
          }}
        >
          Download PNG
        </button>
      </div>
      <div className="eh-card" style={{ height: `${height}px`, position: 'relative' }}>
        {children}
      </div>
    </div>
  );
}

const chartRow = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))',
  gap: '20px',
  marginBottom: '20px',
};

export default function AnalyticsOverviewPage() {
  const [deptExpenses, setDeptExpenses] = useState([]);
  const [travelFreq, setTravelFreq] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [categoryBreakdown, setCategoryBreakdown] = useState([]);
  const [monthlyTrend, setMonthlyTrend] = useState([]);
  const [approvalOutcomes, setApprovalOutcomes] = useState(null);
  const [loading, setLoading] = useState(true);

  const deptChartRef = useRef(null);
  const travelChartRef = useRef(null);
  const categoryChartRef = useRef(null);
  const trendChartRef = useRef(null);
  const approvalChartRef = useRef(null);

  useEffect(() => {
    async function loadData() {
      const [deptRes, freqRes, alertRes, categoryRes, trendRes, approvalRes] = await Promise.all([
        analyticsApi.getDepartmentExpenseComparison(),
        analyticsApi.getEmployeeTravelFrequency(),
        analyticsApi.getBudgetOverrunAlerts(),
        analyticsApi.getExpenseCategoryBreakdown(),
        analyticsApi.getMonthlySpendTrend(),
        analyticsApi.getApprovalOutcomes(),
      ]);
      setDeptExpenses(deptRes.data);
      setTravelFreq(freqRes.data);
      setAlerts(alertRes.data);
      setCategoryBreakdown(categoryRes.data);
      setMonthlyTrend(trendRes.data);
      setApprovalOutcomes(approvalRes.data);
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

      <ChartPanel title="Department Expense Comparison" chartRef={deptChartRef} filename="department-expense-comparison.png" height={240}>
        {deptExpenses.length > 0 && <DepartmentBudgetChart data={deptExpenses} chartRef={deptChartRef} />}
      </ChartPanel>
      <div className="eh-card" style={{ marginBottom: '20px' }}>
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

      <div style={chartRow}>
        <ChartPanel title="Expense Category Breakdown" chartRef={categoryChartRef} filename="expense-category-breakdown.png">
          {categoryBreakdown.length > 0 && <CategoryBreakdownChart data={categoryBreakdown} chartRef={categoryChartRef} />}
        </ChartPanel>
        {approvalOutcomes && (
          <ChartPanel title="Approval Outcome Summary" chartRef={approvalChartRef} filename="approval-outcome-summary.png">
            <ApprovalOutcomeChart
              approved={approvalOutcomes.approved}
              rejected={approvalOutcomes.rejected}
              pending={approvalOutcomes.pending}
              chartRef={approvalChartRef}
            />
          </ChartPanel>
        )}
      </div>

      <div style={chartRow}>
        <ChartPanel title="Monthly Spend Trend" chartRef={trendChartRef} filename="monthly-spend-trend.png">
          {monthlyTrend.length > 0 && <MonthlyTrendChart data={monthlyTrend} chartRef={trendChartRef} />}
        </ChartPanel>
        <ChartPanel title="Employee Travel Frequency" chartRef={travelChartRef} filename="employee-travel-frequency.png">
          {travelFreq.length > 0 && <TravelRankingChart data={travelFreq} chartRef={travelChartRef} />}
        </ChartPanel>
      </div>

      {approvalOutcomes && (
        <div className="eh-card" style={{ marginBottom: '20px' }}>
          <p className="eh-empty">Average turnaround time: {approvalOutcomes.avgTurnaroundHours} hours</p>
        </div>
      )}

      <div className="eh-section-title">Employee Travel Detail</div>
      <div className="eh-card" style={{ marginBottom: '20px' }}>
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
