import { useMemo } from 'react';

const roleInformation = {
  ADMIN: {
    name: 'Administrator',
    shortName: 'ADMIN',
    description:
      'Full administrative access to the Smart Travel & Expense Hub. Administrators are responsible for managing system accounts, roles and overall platform access.',

    permissions: [
      {
        title: 'Account Management',
        description:
          'Create, view, edit, enable and disable system accounts.',
      },
      {
        title: 'Role Management',
        description:
          'Assign appropriate system roles and manage role-based access.',
      },
      {
        title: 'System Administration',
        description:
          'Maintain overall system access and administrative settings.',
      },
      {
        title: 'Analytics & Reporting',
        description:
          'View system analytics and reporting information to monitor platform activity.',
      },
    ],
  },

  FINANCE_STAFF: {
    name: 'Finance Staff',
    shortName: 'FINANCE',
    description:
      'Finance access is focused on reimbursement management, financial review and expense-related processing.',

    permissions: [
      {
        title: 'Reimbursement Management',
        description:
          'Review and manage employee reimbursement submissions.',
      },
      {
        title: 'Expense Review',
        description:
          'Review submitted expense information and supporting details.',
      },
      {
        title: 'Financial Processing',
        description:
          'Process reimbursement records and manage finance-related workflow.',
      },
      {
        title: 'Analytics & Reporting',
        description:
          'Access financial and expense-related analytics and reports.',
      },
    ],
  },

  MANAGER: {
    name: 'Manager',
    shortName: 'MANAGER',
    description:
      'Manager access is focused on reviewing employee requests and handling approval-related workflow.',

    permissions: [
      {
        title: 'Approval Management',
        description:
          'Review reimbursement requests submitted for managerial approval.',
      },
      {
        title: 'Request Review',
        description:
          'Review employee expense and reimbursement information before approval.',
      },
      {
        title: 'Approval Workflow',
        description:
          'Approve or reject requests according to the organisation workflow.',
      },
      {
        title: 'Management Overview',
        description:
          'Monitor relevant requests and approval activities.',
      },
    ],
  },

  EMPLOYEE: {
    name: 'Employee',
    shortName: 'EMPLOYEE',
    description:
      'Employee access is focused on submitting and tracking personal travel and expense-related requests.',

    permissions: [
      {
        title: 'Expense Submission',
        description:
          'Submit travel and expense information for reimbursement.',
      },
      {
        title: 'Reimbursement Tracking',
        description:
          'Track the status of submitted reimbursement requests.',
      },
      {
        title: 'Personal Records',
        description:
          'View your own expense and reimbursement information.',
      },
    ],
  },
};

export default function DashboardPage() {
  const role = localStorage.getItem('role') || 'EMPLOYEE';
  const fullName = localStorage.getItem('fullName') || 'User';

  const currentRole = useMemo(() => {
    return (
      roleInformation[role] ||
      roleInformation.EMPLOYEE
    );
  }, [role]);

  return (
    <section className="dashboard-page">
      <div className="dashboard-container">

        {/* Header */}
        <div className="dashboard-header">
          <div>
            <div className="dashboard-eyebrow">
              SMART TRAVEL & EXPENSE HUB
            </div>

            <h1>
              Welcome, {fullName}
            </h1>

            <p>
              This dashboard provides an overview of your
              role and the system functions available to you.
            </p>
          </div>

          <div className="role-badge-large">
            <span className="role-badge-dot" />
            {currentRole.shortName}
          </div>
        </div>

        {/* Role Overview */}
        <div className="role-overview-card">

          <div className="role-overview-left">
            <div className="role-icon">
              {currentRole.shortName.charAt(0)}
            </div>
          </div>

          <div className="role-overview-content">
            <div className="section-label">
              CURRENT ROLE
            </div>

            <h2>
              {currentRole.name}
            </h2>

            <p>
              {currentRole.description}
            </p>
          </div>

          <div className="access-indicator">
            <span className="access-indicator-dot" />
            Access Enabled
          </div>
        </div>

        {/* Permission Section */}
        <div className="permissions-section">

          <div className="section-heading">
            <div>
              <div className="section-label">
                ACCESS OVERVIEW
              </div>

              <h2>
                Your Available Functions
              </h2>

              <p>
                The functions below are available based on
                your assigned system role.
              </p>
            </div>
          </div>

          <div className="permission-grid">
            {currentRole.permissions.map(
              (permission, index) => (
                <div
                  className="permission-card"
                  key={permission.title}
                >
                  <div className="permission-number">
                    {String(index + 1).padStart(2, '0')}
                  </div>

                  <div className="permission-content">
                    <h3>
                      {permission.title}
                    </h3>

                    <p>
                      {permission.description}
                    </p>
                  </div>

                  <div className="permission-accent" />
                </div>
              ),
            )}
          </div>
        </div>

        {/* Security Notice */}
        <div className="dashboard-notice">

          <div className="notice-icon">
            !
          </div>

          <div>
            <strong>
              Role-based access control
            </strong>

            <p>
              Your available system functions are determined
              by your assigned role. Functions outside your
              permission level are not available.
            </p>
          </div>

        </div>

        {/* Footer */}
        <div className="dashboard-footer">
          <span>
            SMART TRAVEL & EXPENSE HUB
          </span>

          <span className="footer-divider">
            |
          </span>

          <span>
            Role-based Administration Portal
          </span>
        </div>

      </div>

      <style>{`
        .dashboard-page {
          min-height: calc(100vh - 40px);
          background: #f4f6f8;
          padding: 34px;
          box-sizing: border-box;
        }

        .dashboard-container {
          max-width: 1180px;
          margin: 0 auto;
        }

        /* Header */

        .dashboard-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          gap: 30px;
          margin-bottom: 28px;
        }

        .dashboard-eyebrow {
          color: #c39a22;
          font-size: 12px;
          font-weight: 800;
          letter-spacing: 2px;
          margin-bottom: 8px;
        }

        .dashboard-header h1 {
          margin: 0;
          color: #071a2b;
          font-size: 34px;
          line-height: 1.2;
          font-weight: 800;
        }

        .dashboard-header p {
          margin: 10px 0 0;
          color: #667482;
          font-size: 15px;
          line-height: 1.6;
          max-width: 700px;
        }

        .role-badge-large {
          display: flex;
          align-items: center;
          gap: 9px;
          padding: 11px 16px;
          background: #071a2b;
          color: #ffffff;
          border-radius: 8px;
          font-size: 12px;
          font-weight: 800;
          letter-spacing: 1px;
          white-space: nowrap;
          box-shadow: 0 8px 20px rgba(7, 26, 43, 0.15);
        }

        .role-badge-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #e9c34a;
          display: block;
          box-shadow: 0 0 0 4px rgba(233, 195, 74, 0.12);
        }

        /* Role Overview */

        .role-overview-card {
          position: relative;
          display: flex;
          align-items: center;
          gap: 24px;
          padding: 28px 30px;
          margin-bottom: 34px;
          background: #071a2b;
          border-radius: 14px;
          overflow: hidden;
          box-shadow: 0 15px 35px rgba(7, 26, 43, 0.16);
        }

        .role-overview-card::before {
          content: '';
          position: absolute;
          left: 0;
          top: 0;
          bottom: 0;
          width: 5px;
          background: #e9c34a;
        }

        .role-overview-card::after {
          content: '';
          position: absolute;
          width: 260px;
          height: 260px;
          border-radius: 50%;
          right: -120px;
          top: -130px;
          border: 1px solid rgba(233, 195, 74, 0.12);
          box-shadow:
            0 0 0 40px rgba(233, 195, 74, 0.025),
            0 0 0 80px rgba(233, 195, 74, 0.018);
        }

        .role-overview-left {
          position: relative;
          z-index: 1;
        }

        .role-icon {
          width: 64px;
          height: 64px;
          border-radius: 12px;
          background: #e9c34a;
          color: #071a2b;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 27px;
          font-weight: 900;
          box-shadow: 0 8px 20px rgba(233, 195, 74, 0.18);
        }

        .role-overview-content {
          position: relative;
          z-index: 1;
          flex: 1;
        }

        .section-label {
          color: #d2a82e;
          font-size: 11px;
          font-weight: 800;
          letter-spacing: 1.6px;
        }

        .role-overview-content h2 {
          margin: 5px 0 7px;
          color: #ffffff;
          font-size: 25px;
          font-weight: 800;
        }

        .role-overview-content p {
          margin: 0;
          max-width: 760px;
          color: #b9c5d0;
          font-size: 14px;
          line-height: 1.65;
        }

        .access-indicator {
          position: relative;
          z-index: 1;
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 9px 13px;
          border: 1px solid rgba(233, 195, 74, 0.3);
          border-radius: 20px;
          color: #e9c34a;
          font-size: 11px;
          font-weight: 700;
          white-space: nowrap;
        }

        .access-indicator-dot {
          width: 7px;
          height: 7px;
          border-radius: 50%;
          background: #e9c34a;
        }

        /* Permissions */

        .permissions-section {
          margin-bottom: 26px;
        }

        .section-heading {
          margin-bottom: 18px;
        }

        .section-heading h2 {
          margin: 5px 0 4px;
          color: #071a2b;
          font-size: 23px;
          font-weight: 800;
        }

        .section-heading p {
          margin: 0;
          color: #74818e;
          font-size: 14px;
        }

        .permission-grid {
          display: grid;
          grid-template-columns: repeat(2, minmax(0, 1fr));
          gap: 16px;
        }

        .permission-card {
          position: relative;
          display: flex;
          gap: 18px;
          min-height: 145px;
          padding: 22px 24px;
          box-sizing: border-box;
          background: #071a2b;
          border-radius: 12px;
          overflow: hidden;
          box-shadow: 0 8px 20px rgba(7, 26, 43, 0.1);
          transition:
            transform 0.2s ease,
            box-shadow 0.2s ease;
        }

        .permission-card:hover {
          transform: translateY(-2px);
          box-shadow: 0 13px 28px rgba(7, 26, 43, 0.16);
        }

        .permission-number {
          flex-shrink: 0;
          color: #e9c34a;
          font-size: 15px;
          font-weight: 900;
          letter-spacing: 1px;
          padding-top: 2px;
        }

        .permission-content {
          position: relative;
          z-index: 1;
          padding-right: 10px;
        }

        .permission-content h3 {
          margin: 0 0 9px;
          color: #ffffff;
          font-size: 17px;
          font-weight: 750;
        }

        .permission-content p {
          margin: 0;
          color: #aebdcb;
          font-size: 13px;
          line-height: 1.65;
        }

        .permission-accent {
          position: absolute;
          right: 0;
          bottom: 0;
          width: 70px;
          height: 4px;
          background: #e9c34a;
        }

        /* Notice */

        .dashboard-notice {
          display: flex;
          align-items: flex-start;
          gap: 13px;
          padding: 17px 19px;
          margin-top: 20px;
          background: #ffffff;
          border: 1px solid #dce2e8;
          border-left: 4px solid #e9c34a;
          border-radius: 9px;
          box-shadow: 0 5px 15px rgba(7, 26, 43, 0.05);
        }

        .notice-icon {
          width: 24px;
          height: 24px;
          flex-shrink: 0;
          border-radius: 50%;
          background: #e9c34a;
          color: #071a2b;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 13px;
          font-weight: 900;
        }

        .dashboard-notice strong {
          display: block;
          margin-bottom: 4px;
          color: #071a2b;
          font-size: 13px;
          font-weight: 800;
        }

        .dashboard-notice p {
          margin: 0;
          color: #667482;
          font-size: 12px;
          line-height: 1.55;
        }

        /* Footer */

        .dashboard-footer {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 10px;
          margin-top: 28px;
          padding: 5px 0;
          color: #8a96a3;
          font-size: 10px;
          font-weight: 700;
          letter-spacing: 1px;
        }

        .footer-divider {
          color: #c39a22;
        }

        /* Responsive */

        @media (max-width: 900px) {
          .dashboard-page {
            padding: 22px;
          }

          .dashboard-header {
            flex-direction: column;
          }

          .role-badge-large {
            align-self: flex-start;
          }

          .role-overview-card {
            align-items: flex-start;
          }

          .access-indicator {
            display: none;
          }

          .permission-grid {
            grid-template-columns: 1fr;
          }
        }

        @media (max-width: 600px) {
          .dashboard-page {
            padding: 16px;
          }

          .dashboard-header h1 {
            font-size: 28px;
          }

          .role-overview-card {
            flex-direction: column;
            padding: 24px;
          }

          .permission-card {
            padding: 20px;
          }
        }
      `}</style>
    </section>
  );
}
