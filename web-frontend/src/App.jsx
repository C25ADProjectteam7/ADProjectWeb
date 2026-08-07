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
      {/* Public */}
      <Route path="/login" element={<LoginPage />} />

      {/* All authenticated users */}
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<BasicLayout />}>
          <Route index element={<DashboardPage />} />

          {/* Admin only */}
          <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
            <Route
              path="admin/accounts"
              element={<AdminAccountManagementPage />}
            />

            <Route
              path="admin/accounts/create"
              element={<AuthAccountCreationPage />}
            />
          </Route>

          {/* Finance / Manager / Admin */}
          <Route
            element={
              <ProtectedRoute
                allowedRoles={['ADMIN', 'FINANCE_STAFF', 'MANAGER']}
              />
            }
          >
            <Route
              path="finance/reimbursements"
              element={<FinanceReimbursementPage />}
            />
          </Route>

          {/* Manager / Admin */}
          <Route
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'MANAGER']} />
            }
          >
            <Route
              path="manager/approvals"
              element={<ManagerApprovalsPage />}
            />
          </Route>

          {/* All authenticated users */}
          <Route path="analytics" element={<AnalyticsOverviewPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}