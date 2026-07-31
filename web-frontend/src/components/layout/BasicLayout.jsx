import { NavLink, Outlet } from 'react-router-dom';

const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/accounts/create', label: '账号创建与角色' },
  { to: '/admin/accounts', label: '管理员账号管理' },
  { to: '/finance/reimbursements', label: '财务报销流程' },
  { to: '/manager/approvals', label: '经理审批中心' },
  { to: '/analytics', label: '数据分析与可视化' },
];

export default function BasicLayout() {
  return (
    <div>
      <header>
        <h1>Smart Travel and Expense Hub - Web Admin</h1>
        <p>基础页面骨架（无业务逻辑）</p>
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
