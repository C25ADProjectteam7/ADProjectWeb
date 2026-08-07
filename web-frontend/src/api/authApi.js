import apiClient from './client.js';
export const authApi = {
  login(payload) {
    return apiClient.post('/auth/login', payload);
  },
};
