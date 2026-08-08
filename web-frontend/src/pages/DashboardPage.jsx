import { Link } from 'react-router-dom';

export default function DashboardPage() {
  return (
    <section>
      <h2>Dashboard</h2>
              <p>Current placeholder version, only for building module entries and page routes.</p>

      <ul>
        <li>
          <Link to="/accounts/create">Account Creation (Role Permissions)</Link>
        </li>
        <li>
          <Link to="/admin/accounts">Admin Account Management</Link>
        </li>
        <li>
          <Link to="/finance/reimbursements">Financial Reimbursement Process</Link>
        </li>
        <li>
          <Link to="/manager/approvals">Manager Approval Center</Link>
        </li>
        <li>
          <Link to="/analytics">Data Analysis and Visualization</Link>
        </li>
      </ul>
    </section>
  );
}
