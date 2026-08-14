import PropTypes from 'prop-types';
import { Doughnut } from 'react-chartjs-2';
import '../charts/chartSetup.js';

const COLORS = ['#16213E', '#C89B3C', '#4C7A6B', '#E4572E', '#7C8698'];

export default function CategoryBreakdownChart({ data, chartRef }) {
  const chartData = {
    labels: data.map((d) => d.category),
    datasets: [
      {
        data: data.map((d) => d.amount),
        backgroundColor: COLORS.slice(0, data.length),
        borderWidth: 0,
      },
    ],
  };
  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'right' } },
    cutout: '62%',
  };
  return <Doughnut ref={chartRef} data={chartData} options={options} />;
}

CategoryBreakdownChart.propTypes = {
  data: PropTypes.arrayOf(
    PropTypes.shape({
      category: PropTypes.string,
      amount: PropTypes.number,
    }),
  ).isRequired,
  chartRef: PropTypes.object,
};
