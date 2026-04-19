import type { LoginResponseDTO, CurrentUserDTO, PermissionDTO, MenuNodeDTO, LoginUserDTO } from '../dto/auth';
import type { AuthTokens, User, UserPermissions, MenuItem } from '../../models/auth';

export const AuthAdapter = {
  toAuthTokens(dto: LoginResponseDTO): AuthTokens {
    return {
      accessToken: dto.token,
      refreshToken: dto.refreshToken,
      expiresAt: dto.expiresAt ?? (
        typeof dto.expireIn === 'number'
          ? new Date(Date.now() + dto.expireIn * 1000).toISOString()
          : undefined
      ),
    };
  },

  toUser(dto: CurrentUserDTO | LoginUserDTO): User {
    return {
      id: dto.userId,
      username: dto.username,
      nickname: dto.nickName ?? dto.nickname ?? dto.username,
      roles: dto.roleCodes ?? [],
      orgId: dto.orgId,
      userTypeCode: dto.userTypeCode,
    };
  },

  toMenuItem(dto: MenuNodeDTO): MenuItem {
    return {
      id: dto.id,
      parentId: dto.parentId,
      name: dto.name,
      path: dto.path,
      icon: dto.icon,
      children: dto.children?.map(c => this.toMenuItem(c)) || [],
    };
  },

  toUserPermissions(dto: PermissionDTO): UserPermissions {
    return {
      codes: dto.permissions ?? dto.permissionCodes ?? [],
      menus: (dto.menus ?? []).map(m => this.toMenuItem(m)),
    };
  }
};
