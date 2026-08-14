import PropTypes from 'prop-types';
import { Line } from 'react-chartjs-2';
import '../charts/chartSetup.js';

export default function MonthlyTrendChart({ data, chartRef }) {
  const chartData = {
    labels: data.map((d) => d.month),
    datasets: [
      {
        label: 'Monthly spend',
        data: data.map((d) => d.amount),
        borderColor: '#C89B3C',
        backgroundColor: 'rgba(200, 155, 60, 0.12)',
        fill: true,
        tension: 0.3,
        pointBackgroundColor: '#C89B3C',
      },
    ],
  };
  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { x: { grid: { display: false } }, y: { grid: { color: '#F0F1F0' } } },
  };
  return <Line ref={chartRef} data={chartData} options={options} />;
}

MonthlyTrendChart.propTypes = {
  data: PropTypes.arrayOf(
    PropTypes.shape({
      month: PropTypes.string,
      amount: PropTypes.number,
    }),
  ).isRequired,
  chartRef: PropTypes.object,
};
