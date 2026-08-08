import { NavLink, Outlet } from 'react-router-dom';

const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/accounts/create', label: 'Account Creation and Roles' },
  { to: '/admin/accounts', label: 'Admin Account Management' },
  { to: '/finance/reimbursements', label: 'Financial Reimbursement Process' },
  { to: '/manager/approvals', label: 'Manager Approval Center' },
  { to: '/analytics', label: 'Data Analysis and Visualization' },
];

export default function BasicLayout() {
  return (
    <div>
      <header>
        <h1>Smart Travel and Expense Hub - Web Admin</h1>
        <p>Basic Layout Skeleton (No Business Logic)</p>
      </header>

      <nav aria-label="Main navigation">
        <ul>
          {navItems.map((item) => (
            <li key={item.to}>
              <NavLink to={item.to}>{item.label}</NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <hr />

      <main>
        <Outlet />
      </main>
    </div>
  );
}
