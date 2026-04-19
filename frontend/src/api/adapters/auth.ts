import type { LoginResponseDTO, CurrentUserDTO, PermissionDTO, MenuNodeDTO } from '../dto/auth';
import type { AuthTokens, User, UserPermissions, MenuItem } from '../../models/auth';

export const AuthAdapter = {
  toAuthTokens(dto: LoginResponseDTO): AuthTokens {
    return {
      accessToken: dto.token,
      refreshToken: dto.refreshToken,
      expiresAt: dto.expiresAt,
    };
  },

  toUser(dto: CurrentUserDTO): User {
    return {
      id: dto.userId,
      username: dto.username,
      nickname: dto.nickname,
      roles: dto.roleCodes,
      orgId: dto.orgId,
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
      codes: dto.permissionCodes,
      menus: dto.menus.map(m => this.toMenuItem(m)),
    };
  }
};
