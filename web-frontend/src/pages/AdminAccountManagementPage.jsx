export default function AdminAccountManagementPage() {
  return (
    <section>
      <h2>管理员账号管理</h2>
      <p>占位页面：管理员可查看、创建、编辑、禁用、启用其他账号。</p>

      {/* TODO: 接入账号列表、详情抽屉、状态切换和审计记录 */}
      <ul>
        <li>账号列表（分页/搜索）</li>
        <li>编辑账号信息与角色</li>
        <li>禁用与启用操作</li>
      </ul>
    </section>
  );
}
