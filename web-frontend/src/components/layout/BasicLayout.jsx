import { NavLink, Outlet } from 'react-router-dom';
import { getFullName, getRole, logout } from '../../utils/auth.js';

const menuItems = [
  {
    label: 'Dashboard',
    path: '/',
    end: true,
    allowedRoles: ['ADMIN', 'FINANCE_STAFF', 'MANAGER'],
  },
  {
    label: 'Account Management',
    path: '/admin/accounts',
    allowedRoles: ['ADMIN'],
  },
  {
    label: 'Finance',
    path: '/finance/reimbursements',
    allowedRoles: ['ADMIN', 'FINANCE_STAFF'],
  },
  {
    label: 'Manager Approvals',
    path: '/manager/approvals',
    allowedRoles: ['ADMIN', 'MANAGER'],
  },
  {
    label: 'Expense Approvals',
    path: '/manager/expense-approvals',
    allowedRoles: ['ADMIN', 'MANAGER'],
  },
  {
    label: 'Analytics',
    path: '/analytics',
    allowedRoles: ['ADMIN', 'FINANCE_STAFF', 'MANAGER'],
  },
];

function getRoleLabel(role) {
  switch (role) {
    case 'ADMIN':
      return 'System Admin';
    case 'FINANCE_STAFF':
      return 'Finance Staff';
    case 'MANAGER':
      return 'Manager';
    default:
      return 'User';
  }
}

export default function BasicLayout() {
  const fullName = getFullName() || 'Admin';
  const role = getRole();
  const roleLabel = getRoleLabel(role);

  const avatarLetter = fullName.charAt(0).toUpperCase();

  const visibleMenuItems = menuItems.filter((item) => item.allowedRoles.includes(role));

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="brand-mark">S</div>

          <div>
            <div className="brand-title">Smart Travel &</div>
            <div className="brand-subtitle">Expense Hub</div>
          </div>
        </div>

        <div className="sidebar-section-title">MAIN MENU</div>

        <nav className="sidebar-nav">
          {visibleMenuItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.end}
              className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
            >
              <span className="sidebar-link-dot" />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="security-note">
            <div className="security-note-title">SECURE PORTAL</div>

            <div className="security-note-text">Access is controlled by your assigned role.</div>
          </div>
        </div>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <div>
            <div className="topbar-title">Web Administration Portal</div>

            <div className="topbar-subtitle">Smart Travel and Expense Hub</div>
          </div>

          <div className="user-area">
            <div className="user-avatar">{avatarLetter}</div>

            <div className="user-details">
              <div className="user-name">{fullName}</div>

              <div className="user-role">{roleLabel}</div>
            </div>

            <button className="logout-button" onClick={logout}>
              Logout
            </button>
          </div>
        </header>

        <main className="content-area">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
