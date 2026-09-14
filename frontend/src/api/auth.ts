import request from './request';
import type { LoginResponseDTO, CurrentUserDTO, PermissionDTO } from './dto/auth';
import type { ApiResponse } from './dto/base';

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
    return request.post('/auth/login', data);
  },

  requestPasswordReset(username: string): Promise<ApiResponse<PasswordResetRequestDTO>> {
    return request.post('/auth/password-reset/request', { username });
  },

  confirmPasswordReset(data: PasswordResetConfirmPayload): Promise<ApiResponse<PasswordResetConfirmDTO>> {
    return request.post('/auth/password-reset/confirm', data);
  },

  getMe(): Promise<ApiResponse<CurrentUserDTO>> {
    return request.get('/auth/me');
  },

  getPermissions(): Promise<ApiResponse<PermissionDTO>> {
    return request.get('/auth/permissions');
  }
};
