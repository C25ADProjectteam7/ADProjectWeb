import { Navigate, Outlet } from 'react-router-dom';
import PropTypes from 'prop-types';
import { getRole, isAuthenticated } from '../utils/auth.js';

export default function ProtectedRoute({ allowedRoles }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  // 如果没有设置角色限制，只要求登录即可
  if (!allowedRoles || allowedRoles.length === 0) {
    return <Outlet />;
  }

  ProtectedRoute.propTypes = {
    allowedRoles: PropTypes.arrayOf(PropTypes.string),
  };

  const role = getRole();

  // 当前用户没有权限
  if (!allowedRoles.includes(role)) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
