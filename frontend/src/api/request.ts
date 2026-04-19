import axios from 'axios';
import type { AxiosError } from 'axios';
import type { ApiResponse } from './dto/base';

type ErrorResponseBody = Partial<ApiResponse<unknown>>;

export class ApiClientError extends Error {
  code?: string;
  status?: number;
  traceId?: string;
  isNetworkError: boolean;
  isTimeout: boolean;

  constructor(
    message: string,
    options: {
      code?: string;
      status?: number;
      traceId?: string;
      isNetworkError?: boolean;
      isTimeout?: boolean;
      cause?: unknown;
    } = {}
  ) {
    super(message);
    this.name = 'ApiClientError';
    this.code = options.code;
    this.status = options.status;
    this.traceId = options.traceId;
    this.isNetworkError = options.isNetworkError ?? false;
    this.isTimeout = options.isTimeout ?? false;
    if (options.cause !== undefined) {
      (this as Error & { cause?: unknown }).cause = options.cause;
    }
  }
}

function normalizeApiError(error: unknown): ApiClientError {
  if (error instanceof ApiClientError) {
    return error;
  }

  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ErrorResponseBody>;
    const responseBody = axiosError.response?.data;
    const status = axiosError.response?.status;
    const isTimeout = axiosError.code === 'ECONNABORTED';
    const isNetworkError = !axiosError.response;
    const code = responseBody?.code ?? (isNetworkError ? 'NETWORK_ERROR' : undefined);
    const traceId = responseBody?.traceId;

    let message = responseBody?.message?.trim() || axiosError.message || 'Request failed';
    if (isTimeout) {
      message = 'Request timed out';
    } else if (isNetworkError) {
      message = 'Network connection failed';
    } else if (!message && status) {
      message = `Request failed with status ${status}`;
    }

    return new ApiClientError(message, {
      code,
      status,
      traceId,
      isNetworkError,
      isTimeout,
      cause: error,
    });
  }

  if (error instanceof Error) {
    return new ApiClientError(error.message, { cause: error });
  }

  return new ApiClientError('Unknown error');
}

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
  (response): any => {
    const res = response.data as ApiResponse<unknown>;
    // Standard response format: { code: '00000', data: ..., message: ... }
    if (res.code && res.code !== '00000') {
      return Promise.reject(new ApiClientError(res.message || 'Request failed', {
        code: res.code,
        status: response.status,
        traceId: res.traceId,
      }));
    }
    return res;
  },
  (error) => {
    return Promise.reject(normalizeApiError(error));
  }
);

export default request;
