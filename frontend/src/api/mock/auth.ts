import type { ApiResponse } from '../dto/base';
import type { LoginResponseDTO, CurrentUserDTO, PermissionDTO } from '../dto/auth';

export const mockAuthApi = {
  login(): Promise<ApiResponse<LoginResponseDTO>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        token: 'mock-jwt-token-1234567890',
        expireIn: 86400,
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
        user: {
          userId: 1,
          username: 'doctor_admin',
          nickName: 'Dr. Neural',
          roleCodes: ['ROLE_ADMIN', 'ROLE_DOCTOR'],
          orgId: 1001,
          userTypeCode: 'DOCTOR',
        }
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
        nickName: 'Dr. Neural',
        roleCodes: ['ROLE_ADMIN', 'ROLE_DOCTOR'],
        orgId: 1001,
        userTypeCode: 'DOCTOR',
      }
    });
  },

  getPermissions(): Promise<ApiResponse<PermissionDTO>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        userId: 1,
        roles: ['ROLE_ADMIN', 'ROLE_DOCTOR'],
        permissions: ['*:*:*'],
        permissionCodes: ['*:*:*'],
        menus: [
          {
            id: 1,
            parentId: 0,
            name: 'DentAI',
            path: '',
            sort: 1,
            children: [
              { id: 11, parentId: 1, name: '工作台', path: '/dashboard', icon: 'home', sort: 1 },
              { id: 12, parentId: 1, name: 'AI 诊断分析', path: '/ai-diagnosis', icon: 'scan', sort: 2 },
              { id: 13, parentId: 1, name: '病例中心', path: '/cases', icon: 'folder', sort: 3 },
              { id: 14, parentId: 1, name: '数据报表', path: '/reports', icon: 'bar-chart', sort: 4 },
              { id: 15, parentId: 1, name: '用户中心', path: '/user-center', icon: 'user', sort: 5 },
              { id: 16, parentId: 1, name: '系统设置', path: '/settings', icon: 'settings', sort: 6 },
            ]
          }
        ]
      }
    });
  }
};
