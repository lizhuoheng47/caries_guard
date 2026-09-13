import request from './request';
import { mockAuthApi } from './mock/auth';
import type { LoginResponseDTO, CurrentUserDTO, PermissionDTO } from './dto/auth';
import type { ApiResponse } from './dto/base';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

export interface PasswordResetRequestDTO {
  message: string;
  expiresInSeconds: number;
  deliveryMasked: string | null;
  developmentCode: string | null;
}

export interface PasswordResetConfirmDTO {
  reset: boolean;
  message: string;
}

export interface PasswordResetConfirmPayload {
  username: string;
  verificationCode: string;
  newPassword: string;
  confirmPassword: string;
}

export const authApi = {
  login(data: any): Promise<ApiResponse<LoginResponseDTO>> {
    if (USE_MOCK) return mockAuthApi.login();
    return request.post('/auth/login', data);
  },

  requestPasswordReset(username: string): Promise<ApiResponse<PasswordResetRequestDTO>> {
    if (USE_MOCK) return mockAuthApi.requestPasswordReset(username);
    return request.post('/auth/password-reset/request', { username });
  },

  confirmPasswordReset(data: PasswordResetConfirmPayload): Promise<ApiResponse<PasswordResetConfirmDTO>> {
    if (USE_MOCK) return mockAuthApi.confirmPasswordReset(data);
    return request.post('/auth/password-reset/confirm', data);
  },

  getMe(): Promise<ApiResponse<CurrentUserDTO>> {
    if (USE_MOCK) return mockAuthApi.getMe();
    return request.get('/auth/me');
  },

  getPermissions(): Promise<ApiResponse<PermissionDTO>> {
    if (USE_MOCK) return mockAuthApi.getPermissions();
    return request.get('/auth/permissions');
  }
};
