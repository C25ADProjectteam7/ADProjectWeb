import { useEffect, useMemo, useState } from 'react';
import { adminAccountsApi } from '../api/adminAccountsApi.js';

const roleLabels = {
  ADMIN: 'Administrator',
  FINANCE_STAFF: 'Finance Staff',
  MANAGER: 'Manager',
};

const roleDescriptions = {
  ADMIN: 'Full system administration access',
  FINANCE_STAFF: 'Finance and reimbursement management',
  MANAGER: 'Approval and management access',
};

export default function AdminAccountManagementPage() {
  const [accounts, setAccounts] = useState([]);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('success');

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingAccount, setEditingAccount] = useState(null);

  const [form, setForm] = useState({
    email: '',
    password: '',
    fullName: '',
    department: '',
    role: 'FINANCE_STAFF',
  });

  const loadAccounts = async () => {
    try {
      setLoading(true);

      const response = await adminAccountsApi.listAccounts();
      setAccounts(response.data || []);

      setMessage('');
    } catch (error) {
      console.error(error);

      setMessage('Unable to load account information.');
      setMessageType('error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAccounts();
  }, []);

  const filteredAccounts = useMemo(() => {
    const keyword = search.trim().toLowerCase();

    return accounts.filter((account) => {
      const matchesSearch =
        !keyword ||
        account.email?.toLowerCase().includes(keyword) ||
        account.fullName?.toLowerCase().includes(keyword) ||
        account.department?.toLowerCase().includes(keyword);

      const matchesRole =
        roleFilter === 'ALL' || account.role === roleFilter;

      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ENABLED' && account.enabled) ||
        (statusFilter === 'DISABLED' && !account.enabled);

      return matchesSearch && matchesRole && matchesStatus;
    });
  }, [accounts, search, roleFilter, statusFilter]);

  const resetForm = () => {
    setForm({
      email: '',
      password: '',
      fullName: '',
      department: '',
      role: 'FINANCE_STAFF',
    });
  };

  const openCreateForm = () => {
    setEditingAccount(null);
    resetForm();
    setShowCreateForm(true);
    setMessage('');
  };

  const openEditForm = (account) => {
    setEditingAccount(account);

    setForm({
      email: account.email || '',
      password: '',
      fullName: account.fullName || '',
      department: account.department || '',
      role: account.role || 'FINANCE_STAFF',
    });

    setShowCreateForm(true);
    setMessage('');
  };

  const closeForm = () => {
    setShowCreateForm(false);
    setEditingAccount(null);
    resetForm();
  };

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      if (editingAccount) {
        const payload = {
          fullName: form.fullName,
          department: form.department,
          role: form.role,
        };

        if (form.password.trim()) {
          payload.password = form.password;
        }

        await adminAccountsApi.updateAccount(
          editingAccount.id,
          payload,
        );

        setMessage('Account updated successfully.');
        setMessageType('success');
      } else {
        await adminAccountsApi.createAccount({
          email: form.email,
          password: form.password,
          fullName: form.fullName,
          department: form.department,
          role: form.role,
        });

        setMessage('Account created successfully.');
        setMessageType('success');
      }

      closeForm();
      await loadAccounts();
    } catch (error) {
      console.error(error);

      const serverMessage =
        error.response?.data?.message ||
        'The account operation could not be completed.';

      setMessage(serverMessage);
      setMessageType('error');
    }
  };

  const handleStatusChange = async (account) => {
    const action = account.enabled ? 'disable' : 'enable';

    const confirmed = window.confirm(
      `Are you sure you want to ${action} ${account.email}?`,
    );

    if (!confirmed) {
      return;
    }

    try {
      await adminAccountsApi.updateAccountStatus(
        account.id,
        !account.enabled,
      );

      setMessage(
        `${account.email} has been ${action}d successfully.`,
      );
      setMessageType('success');

      await loadAccounts();
    } catch (error) {
      console.error(error);

      setMessage(
        `Unable to ${action} this account.`,
      );
      setMessageType('error');
    }
  };

  return (
    <section className="account-page">
      <div className="page-header">
        <div>
          <div className="page-eyebrow">ADMINISTRATION</div>

          <h1>Account Management</h1>

          <p>
            Create, view, edit, enable and disable system accounts.
            Role assignments determine access to different modules.
          </p>
        </div>

        <button
          className="primary-button"
          onClick={openCreateForm}
        >
          + Create Account
        </button>
      </div>

      {message && (
        <div
          className={`system-message ${
            messageType === 'error'
              ? 'system-message-error'
              : 'system-message-success'
          }`}
        >
          <span className="message-icon">
            {messageType === 'error' ? '!' : '✓'}
          </span>

          <span>{message}</span>
        </div>
      )}

      <div className="account-summary">
        <div className="summary-card">
          <span className="summary-label">TOTAL ACCOUNTS</span>
          <strong>{accounts.length}</strong>
        </div>

        <div className="summary-card">
          <span className="summary-label">ACTIVE</span>
          <strong>
            {accounts.filter((account) => account.enabled).length}
          </strong>
        </div>

        <div className="summary-card">
          <span className="summary-label">DISABLED</span>
          <strong>
            {accounts.filter((account) => !account.enabled).length}
          </strong>
        </div>

        <div className="summary-card">
          <span className="summary-label">ADMINISTRATORS</span>
          <strong>
            {
              accounts.filter(
                (account) => account.role === 'ADMIN',
              ).length
            }
          </strong>
        </div>
      </div>

      <div className="account-panel">
        <div className="panel-header">
          <div>
            <h2>System Accounts</h2>
            <span>
              {filteredAccounts.length} account
              {filteredAccounts.length === 1 ? '' : 's'} shown
            </span>
          </div>
        </div>

        <div className="filter-bar">
          <input
            className="search-input"
            type="text"
            placeholder="Search by name, email or department..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />

          <select
            className="filter-select"
            value={roleFilter}
            onChange={(event) =>
              setRoleFilter(event.target.value)
            }
          >
            <option value="ALL">All Roles</option>
            <option value="ADMIN">Administrator</option>
            <option value="FINANCE_STAFF">Finance Staff</option>
            <option value="MANAGER">Manager</option>
          </select>

          <select
            className="filter-select"
            value={statusFilter}
            onChange={(event) =>
              setStatusFilter(event.target.value)
            }
          >
            <option value="ALL">All Status</option>
            <option value="ENABLED">Enabled</option>
            <option value="DISABLED">Disabled</option>
          </select>
        </div>

        {loading ? (
          <div className="empty-state">
            Loading account information...
          </div>
        ) : filteredAccounts.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">?</div>

            <h3>No accounts found</h3>

            <p>
              Try changing the search or filter settings.
            </p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="account-table">
              <thead>
                <tr>
                  <th>ACCOUNT</th>
                  <th>DEPARTMENT</th>
                  <th>ROLE</th>
                  <th>STATUS</th>
                  <th>FAILED LOGINS</th>
                  <th>ACTIONS</th>
                </tr>
              </thead>

              <tbody>
                {filteredAccounts.map((account) => {
                  const avatarLetter =
                    account.fullName?.charAt(0).toUpperCase() ||
                    account.email?.charAt(0).toUpperCase() ||
                    '?';

                  return (
                    <tr key={account.id}>
                      <td>
                        <div className="account-identity">
                          <div className="account-avatar">
                            {avatarLetter}
                          </div>

                          <div>
                            <div className="account-name">
                              {account.fullName}
                            </div>

                            <div className="account-email">
                              {account.email}
                            </div>
                          </div>
                        </div>
                      </td>

                      <td>
                        {account.department || '—'}
                      </td>

                      <td>
                        <div className="role-cell">
                          <span
                            className={`role-badge role-${account.role}`}
                          >
                            {roleLabels[account.role] ||
                              account.role}
                          </span>

                          <small>
                            {roleDescriptions[account.role]}
                          </small>
                        </div>
                      </td>

                      <td>
                        <span
                          className={`status-badge ${
                            account.enabled
                              ? 'status-enabled'
                              : 'status-disabled'
                          }`}
                        >
                          <span className="status-dot" />

                          {account.enabled
                            ? 'Enabled'
                            : 'Disabled'}
                        </span>
                      </td>

                      <td>
                        <span
                          className={
                            account.failedLoginAttempts > 0
                              ? 'failed-login-warning'
                              : 'failed-login-normal'
                          }
                        >
                          {account.failedLoginAttempts}
                        </span>
                      </td>

                      <td>
                        <div className="action-buttons">
                          <button
                            className="secondary-button"
                            onClick={() =>
                              openEditForm(account)
                            }
                          >
                            Edit
                          </button>

                          <button
                            className={
                              account.enabled
                                ? 'danger-button'
                                : 'success-button'
                            }
                            onClick={() =>
                              handleStatusChange(account)
                            }
                          >
                            {account.enabled
                              ? 'Disable'
                              : 'Enable'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showCreateForm && (
        <div
          className="modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeForm();
            }
          }}
        >
          <div className="account-modal">
            <div className="modal-header">
              <div>
                <div className="page-eyebrow">
                  {editingAccount
                    ? 'EDIT ACCOUNT'
                    : 'NEW ACCOUNT'}
                </div>

                <h2>
                  {editingAccount
                    ? 'Edit Account'
                    : 'Create Account'}
                </h2>

                <p>
                  Assign account information and system role.
                </p>
              </div>

              <button
                className="modal-close"
                onClick={closeForm}
              >
                ×
              </button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Full Name</label>

                  <input
                    name="fullName"
                    value={form.fullName}
                    onChange={handleChange}
                    placeholder="Enter full name"
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Email Address</label>

                  <input
                    name="email"
                    type="email"
                    value={form.email}
                    onChange={handleChange}
                    placeholder="name@example.com"
                    disabled={Boolean(editingAccount)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Department</label>

                  <input
                    name="department"
                    value={form.department}
                    onChange={handleChange}
                    placeholder="e.g. Finance"
                  />
                </div>

                <div className="form-group">
                  <label>
                    {editingAccount
                      ? 'New Password'
                      : 'Password'}
                  </label>

                  <input
                    name="password"
                    type="password"
                    value={form.password}
                    onChange={handleChange}
                    placeholder={
                      editingAccount
                        ? 'Leave blank to keep current password'
                        : 'Minimum 6 characters'
                    }
                    required={!editingAccount}
                  />
                </div>

                <div className="form-group form-group-full">
                  <label>System Role</label>

                  <select
                    name="role"
                    value={form.role}
                    onChange={handleChange}
                    required
                  >
                    <option value="FINANCE_STAFF">
                      Finance Staff
                    </option>

                    <option value="MANAGER">
                      Manager
                    </option>

                    <option value="ADMIN">
                      Administrator
                    </option>
                  </select>
                </div>
              </div>

              <div className="role-permission-preview">
                <div className="preview-title">
                  ROLE PERMISSION PREVIEW
                </div>

                <div className="permission-card">
                  <strong>
                    {roleLabels[form.role]}
                  </strong>

                  <span>
                    {roleDescriptions[form.role]}
                  </span>
                </div>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="secondary-button large-button"
                  onClick={closeForm}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="success-button large-button"
                >
                  {editingAccount
                    ? 'Save Changes'
                    : 'Create Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}