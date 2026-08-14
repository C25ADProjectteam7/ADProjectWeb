import PropTypes from 'prop-types';
import { Bar } from 'react-chartjs-2';
import '../charts/chartSetup.js';

export default function TravelRankingChart({ data, chartRef }) {
  const chartData = {
    labels: data.map((d) => d.userName),
    datasets: [
      {
        label: 'Trips',
        data: data.map((d) => d.tripCount),
        backgroundColor: '#3A4767',
        borderRadius: 4,
      },
    ],
  };
  const options = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { color: '#F0F1F0' }, ticks: { precision: 0 } },
      y: { grid: { display: false } },
    },
  };
  return <Bar ref={chartRef} data={chartData} options={options} />;
}

TravelRankingChart.propTypes = {
  data: PropTypes.arrayOf(
    PropTypes.shape({
      userName: PropTypes.string,
      tripCount: PropTypes.number,
    }),
  ).isRequired,
  chartRef: PropTypes.object,
};
