import type { ApiResponse } from '../dto/base';
import type { LoginResponseDTO, CurrentUserDTO, PermissionDTO } from '../dto/auth';

export const mockAuthApi = {
  login(): Promise<ApiResponse<LoginResponseDTO>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        token: 'mock-jwt-token-1234567890',
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
      }
    });
  },

  getMe(): Promise<ApiResponse<CurrentUserDTO>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        userId: 1,
        username: 'doctor_admin',
        nickname: 'Dr. Neural',
        roleCodes: ['ROLE_ADMIN', 'ROLE_DOCTOR'],
        orgId: 1001,
      }
    });
  },

  getPermissions(): Promise<ApiResponse<PermissionDTO>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        permissionCodes: ['*:*:*'],
        menus: [
          {
            id: 1,
            parentId: 0,
            name: 'AI CORE',
            path: '',
            sort: 1,
            children: [
              { id: 11, parentId: 1, name: '影像扫描', path: '/scan', icon: 'scan', sort: 1 },
              { id: 12, parentId: 1, name: '分析队列', path: '/analysis', icon: 'list', sort: 2 },
              { id: 13, parentId: 1, name: '医生复核', path: '/review', icon: 'check-square', sort: 3 },
              { id: 14, parentId: 1, name: '智能解释', path: '/rag', icon: 'message-square', sort: 4 },
            ]
          },
          {
            id: 2,
            parentId: 0,
            name: 'INTELLIGENCE',
            path: '',
            sort: 2,
            children: [
              { id: 21, parentId: 2, name: 'AI 评估看板', path: '/dashboard/ai', icon: 'activity', sort: 1 },
              { id: 22, parentId: 2, name: '知识图库', path: '/knowledge', icon: 'book', sort: 2 },
            ]
          }
        ]
      }
    });
  }
};
