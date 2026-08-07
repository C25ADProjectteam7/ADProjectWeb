import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { adminAccountsApi } from '../api/adminAccountsApi.js';

export default function AuthAccountCreationPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    email: '',
    password: '',
    fullName: '',
    department: '',
    role: 'FINANCE_STAFF',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  function handleChange(event) {
    const { name, value } = event.target;

    setForm({
      ...form,
      [name]: value,
    });
  }

  async function handleSubmit(event) {
    event.preventDefault();

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await adminAccountsApi.createAccount(form);

      console.log('Created account:', response.data);

      setSuccess(`Account ${response.data.email} was created successfully.`);

      setForm({
        email: '',
        password: '',
        fullName: '',
        department: '',
        role: 'FINANCE_STAFF',
      });

      setTimeout(() => {
        navigate('/admin/accounts');
      }, 1000);
    } catch (err) {
      console.error(err);

      setError(err.response?.data?.message || 'Failed to create account.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <section>
      <div style={{ marginBottom: '20px' }}>
        <Link to="/admin/accounts">← Back to Account Management</Link>
      </div>

      <h2>Account Creation</h2>

      <p>Create a new web account and assign the appropriate role.</p>

      {error && (
        <div
          style={{
            marginBottom: '15px',
            padding: '10px',
            border: '1px solid #dc3545',
          }}
        >
          {error}
        </div>
      )}

      {success && (
        <div
          style={{
            marginBottom: '15px',
            padding: '10px',
            border: '1px solid #198754',
          }}
        >
          {success}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '15px' }}>
          <label>
            Email
            <br />
            <input type="email" name="email" value={form.email} onChange={handleChange} required />
          </label>
        </div>

        <div style={{ marginBottom: '15px' }}>
          <label>
            Password
            <br />
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              minLength="6"
              required
            />
          </label>

          <small>Password must contain at least 6 characters.</small>
        </div>

        <div style={{ marginBottom: '15px' }}>
          <label>
            Full Name
            <br />
            <input
              type="text"
              name="fullName"
              value={form.fullName}
              onChange={handleChange}
              required
            />
          </label>
        </div>

        <div style={{ marginBottom: '15px' }}>
          <label>
            Department
            <br />
            <input
              type="text"
              name="department"
              value={form.department}
              onChange={handleChange}
              placeholder="e.g. Finance"
            />
          </label>
        </div>

        <div style={{ marginBottom: '15px' }}>
          <label>
            Role
            <br />
            <select name="role" value={form.role} onChange={handleChange}>
              <option value="FINANCE_STAFF">Finance Staff</option>

              <option value="MANAGER">Manager</option>

              <option value="ADMIN">Admin</option>
            </select>
          </label>
        </div>

        <div>
          <button type="submit" disabled={loading}>
            {loading ? 'Creating...' : 'Create Account'}
          </button>

          <button
            type="button"
            onClick={() => navigate('/admin/accounts')}
            style={{ marginLeft: '8px' }}
          >
            Cancel
          </button>
        </div>
      </form>
    </section>
  );
}
