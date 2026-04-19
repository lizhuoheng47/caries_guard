import axios from 'axios'

export const apiClient = axios.create({
  baseURL: '/api/v1/rag',
  timeout: 15000,
})

apiClient.interceptors.response.use((response) => response.data.data)
