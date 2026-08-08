import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi.js';

export default function LoginPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!email.trim() || !password) {
      setError('Please enter your email and password.');
      return;
    }

    try {
      setLoading(true);

      const response = await authApi.login({
        email: email.trim(),
        password,
      });

      const data = response.data;

      localStorage.setItem('accessToken', data.accessToken);

      if (data.role) {
        localStorage.setItem('role', data.role);
      }

      if (data.fullName) {
        localStorage.setItem('fullName', data.fullName);
      }

      navigate('/', { replace: true });
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data ||
        'Login failed. Please check your email and password.';

      setError(
        typeof message === 'string'
          ? message
          : 'Login failed. Please check your email and password.',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.backgroundShapeOne} />
      <div style={styles.backgroundShapeTwo} />

      <main style={styles.container}>
        <section style={styles.brandPanel}>
          <div style={styles.logoCircle}>S</div>

          <div style={styles.brandTitle}>Smart Travel</div>

          <div style={styles.brandSubtitle}>& Expense Hub</div>

          <div style={styles.brandDescription}>Web Administration Portal</div>

          <div style={styles.featureList}>
            <div style={styles.featureItem}>
              <span style={styles.featureIcon}>✓</span>
              <span>Account management</span>
            </div>

            <div style={styles.featureItem}>
              <span style={styles.featureIcon}>✓</span>
              <span>Expense management</span>
            </div>

            <div style={styles.featureItem}>
              <span style={styles.featureIcon}>✓</span>
              <span>Approval workflow</span>
            </div>

            <div style={styles.featureItem}>
              <span style={styles.featureIcon}>✓</span>
              <span>Analytics and reporting</span>
            </div>
          </div>

          <div style={styles.roleBox}>
            <div style={styles.roleTitle}>Role-based access</div>

            <div style={styles.roleList}>
              <span style={styles.roleTag}>Admin</span>
              <span style={styles.roleTag}>Finance Staff</span>
              <span style={styles.roleTag}>Manager</span>
            </div>
          </div>
        </section>

        <section style={styles.loginPanel}>
          <div style={styles.loginCard}>
            <div style={styles.avatar}>S</div>

            <div style={styles.welcome}>WELCOME BACK</div>

            <h1 style={styles.title}>Sign in</h1>

            <p style={styles.description}>Sign in to access the administration portal.</p>

            {error && (
              <div style={styles.errorBox}>
                <span style={styles.errorIcon}>!</span>
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit}>
              <label style={styles.label} htmlFor="email">
                Email address
              </label>

              <input
                id="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="Enter your email"
                autoComplete="email"
                style={styles.input}
              />

              <label style={styles.label} htmlFor="password">
                Password
              </label>

              <input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter your password"
                autoComplete="current-password"
                style={styles.input}
              />

              <button
                type="submit"
                disabled={loading}
                style={{
                  ...styles.button,
                  ...(loading ? styles.buttonDisabled : {}),
                }}
              >
                {loading ? 'Signing in...' : 'Sign in'}
              </button>
            </form>

            <div style={styles.securityNotice}>
              <span style={styles.securityIcon}>●</span>

              <span>Secure access · Role-based permissions enabled</span>
            </div>
          </div>

          <div style={styles.footer}>Smart Travel & Expense Hub · Administration Portal</div>
        </section>
      </main>
    </div>
  );
}

const styles = {
  page: {
    minHeight: '100vh',
    background: '#07111f',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '32px',
    boxSizing: 'border-box',
    position: 'relative',
    overflow: 'hidden',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif',
  },

  backgroundShapeOne: {
    position: 'absolute',
    width: '520px',
    height: '520px',
    borderRadius: '50%',
    background: 'rgba(24, 74, 111, 0.25)',
    top: '-260px',
    left: '-180px',
  },

  backgroundShapeTwo: {
    position: 'absolute',
    width: '420px',
    height: '420px',
    borderRadius: '50%',
    background: 'rgba(19, 73, 58, 0.22)',
    bottom: '-220px',
    right: '-150px',
  },

  container: {
    width: '100%',
    maxWidth: '1120px',
    minHeight: '650px',
    display: 'grid',
    gridTemplateColumns: '0.95fr 1.05fr',
    position: 'relative',
    zIndex: 1,
    borderRadius: '22px',
    overflow: 'hidden',
    boxShadow: '0 25px 70px rgba(0, 0, 0, 0.35)',
    background: '#ffffff',
  },

  brandPanel: {
    background: 'linear-gradient(145deg, #07111f 0%, #0b1d31 55%, #102a40 100%)',
    color: '#ffffff',
    padding: '64px 56px',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
  },

  logoCircle: {
    width: '62px',
    height: '62px',
    borderRadius: '50%',
    background: '#e9c34a',
    color: '#07111f',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '30px',
    fontWeight: 800,
    marginBottom: '24px',
    boxShadow: '0 8px 25px rgba(233, 195, 74, 0.2)',
  },

  brandTitle: {
    fontSize: '36px',
    fontWeight: 800,
    lineHeight: 1.1,
    letterSpacing: '-1px',
  },

  brandSubtitle: {
    fontSize: '36px',
    fontWeight: 800,
    lineHeight: 1.1,
    color: '#e9c34a',
    letterSpacing: '-1px',
  },

  brandDescription: {
    marginTop: '18px',
    color: '#aebdcb',
    fontSize: '16px',
    letterSpacing: '0.3px',
  },

  featureList: {
    marginTop: '42px',
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },

  featureItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    color: '#d9e3ec',
    fontSize: '15px',
  },

  featureIcon: {
    width: '25px',
    height: '25px',
    borderRadius: '50%',
    background: '#164e3c',
    color: '#ffffff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '13px',
    fontWeight: 700,
  },

  roleBox: {
    marginTop: '38px',
    padding: '16px 18px',
    borderRadius: '12px',
    background: 'rgba(255, 255, 255, 0.06)',
    border: '1px solid rgba(255, 255, 255, 0.1)',
  },

  roleTitle: {
    color: '#e9c34a',
    fontSize: '12px',
    fontWeight: 800,
    textTransform: 'uppercase',
    letterSpacing: '1px',
    marginBottom: '10px',
  },

  roleList: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '8px',
  },

  roleTag: {
    padding: '6px 10px',
    borderRadius: '20px',
    background: 'rgba(255, 255, 255, 0.08)',
    border: '1px solid rgba(255, 255, 255, 0.12)',
    color: '#d9e3ec',
    fontSize: '12px',
    fontWeight: 600,
  },

  loginPanel: {
    background: '#ffffff',
    padding: '48px 70px',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
  },

  loginCard: {
    width: '100%',
    maxWidth: '430px',
    margin: '0 auto',
  },

  avatar: {
    width: '54px',
    height: '54px',
    borderRadius: '50%',
    background: '#e9c34a',
    color: '#07111f',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '24px',
    fontWeight: 800,
    marginBottom: '20px',
  },

  welcome: {
    color: '#164e3c',
    fontSize: '13px',
    fontWeight: 800,
    marginBottom: '7px',
    letterSpacing: '1.5px',
  },

  title: {
    margin: 0,
    color: '#07111f',
    fontSize: '38px',
    lineHeight: 1.15,
    fontWeight: 800,
  },

  description: {
    marginTop: '10px',
    marginBottom: '30px',
    color: '#667482',
    fontSize: '15px',
    lineHeight: 1.6,
  },

  errorBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '12px 14px',
    marginBottom: '20px',
    borderRadius: '10px',
    background: '#fff8df',
    border: '1px solid #e9c34a',
    color: '#463a08',
    fontSize: '14px',
    lineHeight: 1.4,
  },

  errorIcon: {
    width: '22px',
    height: '22px',
    minWidth: '22px',
    borderRadius: '50%',
    background: '#e9c34a',
    color: '#07111f',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 800,
  },

  label: {
    display: 'block',
    color: '#172333',
    fontSize: '14px',
    fontWeight: 700,
    marginBottom: '8px',
    marginTop: '18px',
  },

  input: {
    width: '100%',
    boxSizing: 'border-box',
    height: '48px',
    border: '1px solid #d7dee6',
    borderRadius: '9px',
    padding: '0 14px',
    fontSize: '15px',
    color: '#07111f',
    outline: 'none',
    background: '#ffffff',
  },

  button: {
    width: '100%',
    height: '50px',
    marginTop: '28px',
    border: 'none',
    borderRadius: '9px',
    background: '#164e3c',
    color: '#ffffff',
    fontSize: '15px',
    fontWeight: 700,
    cursor: 'pointer',
    boxShadow: '0 8px 18px rgba(22, 78, 60, 0.22)',
  },

  buttonDisabled: {
    opacity: 0.65,
    cursor: 'not-allowed',
  },

  securityNotice: {
    display: 'flex',
    alignItems: 'center',
    gap: '9px',
    marginTop: '24px',
    padding: '12px 14px',
    background: '#f4f6f8',
    borderRadius: '9px',
    color: '#4d5b68',
    fontSize: '12px',
  },

  securityIcon: {
    color: '#164e3c',
    fontSize: '8px',
  },

  footer: {
    textAlign: 'center',
    marginTop: '28px',
    color: '#8a96a3',
    fontSize: '12px',
  },
};
