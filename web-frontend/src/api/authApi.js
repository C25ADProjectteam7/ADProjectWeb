import apiClient from './client.js';

// TODO: 账号创建与角色权限接口占位（后续对接后端真实 API）
export const authApi = {
  createAccount(payload) {
    return apiClient.post('/auth/accounts', payload);
  },
};
