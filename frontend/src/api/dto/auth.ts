export interface LoginResponseDTO {
  token: string;
  refreshToken?: string;
  expiresAt?: string;
}

export interface CurrentUserDTO {
  userId: number;
  username: string;
  nickname: string;
  roleCodes: string[];
  orgId: number;
}

export interface MenuNodeDTO {
  id: number;
  parentId: number;
  name: string;
  path: string;
  icon?: string;
  sort: number;
  children?: MenuNodeDTO[];
}

export interface PermissionDTO {
  permissionCodes: string[];
  menus: MenuNodeDTO[];
}
