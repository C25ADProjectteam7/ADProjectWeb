import axios from 'axios';

// 统一 API 客户端：baseURL 由环境变量控制
// 生产环境经 Nginx 网关转发至 Spring Boot 后端 (backlog Item 29)
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
});

// 统一注入鉴权 Token (对接 Item 1: JWT 登录)
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 统一错误处理，例如 401 时跳转登录
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // TODO: 跳转登录页 / 刷新 Token
    }
    return Promise.reject(error);
  },
);

export default apiClient;
