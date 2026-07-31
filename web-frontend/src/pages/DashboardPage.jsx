import { Link } from 'react-router-dom';

export default function DashboardPage() {
  return (
    <section>
      <h2>Dashboard</h2>
      <p>当前为占位版本，仅用于搭建模块入口和页面路由。</p>

      <ul>
        <li>
          <Link to="/accounts/create">账号创建（角色权限）</Link>
        </li>
        <li>
          <Link to="/admin/accounts">管理员账号管理</Link>
        </li>
        <li>
          <Link to="/finance/reimbursements">财务报销流程</Link>
        </li>
        <li>
          <Link to="/manager/approvals">经理审批中心</Link>
        </li>
        <li>
          <Link to="/analytics">数据分析与可视化</Link>
        </li>
      </ul>
    </section>
  );
}
