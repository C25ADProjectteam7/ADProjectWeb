import { Bar } from 'react-chartjs-2';
import '../charts/chartSetup.js';

function barColor(actual, budget) {
  const ratio = actual / budget;
  if (ratio > 1) return '#E4572E';
  if (ratio >= 0.85) return '#C89B3C';
  return '#4C7A6B';
}

export default function DepartmentBudgetChart({ data, chartRef }) {
  const chartData = {
    labels: data.map((d) => d.department),
    datasets: [
      { label: 'Budget', data: data.map((d) => d.budget), backgroundColor: '#E2E5E4', borderRadius: 4 },
      {
        label: 'Actual spend',
        data: data.map((d) => d.totalExpense),
        backgroundColor: data.map((d) => barColor(d.totalExpense, d.budget)),
        borderRadius: 4,
      },
    ],
  };
  const options = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } },
    scales: { x: { grid: { color: '#F0F1F0' } }, y: { grid: { display: false } } },
  };
  return <Bar ref={chartRef} data={chartData} options={options} />;
}