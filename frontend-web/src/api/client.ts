import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'

type ApiEnvelope<T> = {
  code?: string
  data?: T
  message?: string
}

type ApiClient = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const unwrap = <T>(request: Promise<AxiosResponse<ApiEnvelope<T> | T>>): Promise<T> =>
  request.then((response) => {
    const payload = response.data
    if (payload && typeof payload === 'object' && 'data' in payload) {
      return payload.data as T
    }
    return payload as T
  })

export const createApiClient = (baseURL: string): ApiClient => {
  const client = axios.create({
    baseURL,
    timeout: 15000,
  })

  return {
    get: <T = any>(url: string, config?: AxiosRequestConfig) => unwrap<T>(client.get(url, config)),
    post: <T = any>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
      unwrap<T>(client.post(url, data, config)),
    put: <T = any>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
      unwrap<T>(client.put(url, data, config)),
    delete: <T = any>(url: string, config?: AxiosRequestConfig) => unwrap<T>(client.delete(url, config)),
  }
}
