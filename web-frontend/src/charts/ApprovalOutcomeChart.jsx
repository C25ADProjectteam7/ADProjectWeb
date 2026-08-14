import PropTypes from 'prop-types';
import { Doughnut } from 'react-chartjs-2';
import '../charts/chartSetup.js';

export default function ApprovalOutcomeChart({ approved, rejected, pending, chartRef }) {
  const chartData = {
    labels: ['Approved', 'Rejected', 'Pending'],
    datasets: [
      {
        data: [approved, rejected, pending],
        backgroundColor: ['#4C7A6B', '#E4572E', '#C89B3C'],
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

ApprovalOutcomeChart.propTypes = {
  approved: PropTypes.number.isRequired,
  rejected: PropTypes.number.isRequired,
  pending: PropTypes.number.isRequired,
  chartRef: PropTypes.object,
};
