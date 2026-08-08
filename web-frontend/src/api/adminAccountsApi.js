import apiClient from './client.js';

export const adminAccountsApi = {
  listAccounts(params) {
    return apiClient.get('/admin/users', { params });
  },

  createAccount(payload) {
    return apiClient.post('/admin/users', payload);
  },

  updateAccount(accountId, payload) {
    return apiClient.put(`/admin/users/${accountId}`, payload);
  },

  updateAccountStatus(accountId, enabled) {
    return apiClient.patch(`/admin/users/${accountId}/status`, null, {
      params: { enabled },
    });
  },
};
