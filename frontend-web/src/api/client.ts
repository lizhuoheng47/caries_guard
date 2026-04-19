import axios from 'axios'

export const createApiClient = (baseURL: string) => {
  const client = axios.create({
    baseURL,
    timeout: 15000,
  })

  client.interceptors.response.use((response) => response.data.data)
  return client
}
