import { Route, Routes } from 'react-router-dom';
import BasicLayout from './components/layout/BasicLayout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import AdminAccountManagementPage from './pages/AdminAccountManagementPage.jsx';
import AnalyticsOverviewPage from './pages/AnalyticsOverviewPage.jsx';
import AuthAccountCreationPage from './pages/AuthAccountCreationPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import FinanceReimbursementPage from './pages/FinanceReimbursementPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import ManagerApprovalsPage from './pages/ManagerApprovalsPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<BasicLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="accounts/create" element={<AuthAccountCreationPage />} />
          <Route path="admin/accounts" element={<AdminAccountManagementPage />} />
          <Route path="finance/reimbursements" element={<FinanceReimbursementPage />} />
          <Route path="manager/approvals" element={<ManagerApprovalsPage />} />
          <Route path="analytics" element={<AnalyticsOverviewPage />} />
        </Route>
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
