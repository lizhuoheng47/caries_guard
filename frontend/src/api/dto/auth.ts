export interface LoginUserDTO {
  userId: number;
  username: string;
  nickName?: string;
  nickname?: string;
  roleCodes?: string[];
  orgId: number;
  userTypeCode?: string;
}

export interface LoginResponseDTO {
  token: string;
  expireIn?: number;
  refreshToken?: string;
  expiresAt?: string;
  user?: LoginUserDTO;
}

export interface CurrentUserDTO {
  userId: number;
  username: string;
  nickName?: string;
  nickname?: string;
  roleCodes?: string[];
  orgId: number;
  userTypeCode?: string;
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
  userId?: number;
  roles?: string[];
  permissions?: string[];
  permissionCodes?: string[];
  menus?: MenuNodeDTO[];
}
