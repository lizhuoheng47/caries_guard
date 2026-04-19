import axios from 'axios';

// The base request instance
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
});

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    // Standard response format: { code: '00000', data: ..., message: ... }
    if (res.code && res.code !== '00000') {
      // Handle error
      return Promise.reject(new Error(res.message || 'Error'));
    }
    return res;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default request;
