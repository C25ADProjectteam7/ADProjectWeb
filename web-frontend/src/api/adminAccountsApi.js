import apiClient from './client.js';

// TODO: 管理员账号管理接口占位（查看、创建、编辑、禁用、启用）
export const adminAccountsApi = {
  listAccounts(params) {
    return apiClient.get('/admin/accounts', { params });
  },
  createAccount(payload) {
    return apiClient.post('/admin/accounts', payload);
  },
  updateAccount(accountId, payload) {
    return apiClient.patch(`/admin/accounts/${accountId}`, payload);
  },
  disableAccount(accountId) {
    return apiClient.post(`/admin/accounts/${accountId}/disable`);
  },
  enableAccount(accountId) {
    return apiClient.post(`/admin/accounts/${accountId}/enable`);
  },
};
