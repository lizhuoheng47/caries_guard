import request from './request';
import { mockAuthApi } from './mock/auth';
import type { LoginResponseDTO, CurrentUserDTO, PermissionDTO } from './dto/auth';
import type { ApiResponse } from './dto/base';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

export const authApi = {
  login(data: any): Promise<ApiResponse<LoginResponseDTO>> {
    if (USE_MOCK) return mockAuthApi.login();
    return request.post('/auth/login', data);
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
