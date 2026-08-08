import apiClient from './client.js';

export const authApi = {
  createAccount(payload) {
    return apiClient.post('/auth/accounts', payload);
  },
};
