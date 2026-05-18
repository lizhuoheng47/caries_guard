export interface LoginUserDTO {
  userId: number;
  username: string;
  nickName?: string;
  nickname?: string;
  realNameMasked?: string;
  deptId?: number;
  userNo?: string;
  phoneMasked?: string;
  emailMasked?: string;
  avatarUrl?: string;
  roleCodes?: string[];
  orgId: number;
  userTypeCode?: string;
  genderCode?: string;
  certificateNoMasked?: string;
  lastLoginAt?: string;
  status?: string;
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
  realNameMasked?: string;
  deptId?: number;
  userNo?: string;
  phoneMasked?: string;
  emailMasked?: string;
  avatarUrl?: string;
  roleCodes?: string[];
  orgId: number;
  userTypeCode?: string;
  genderCode?: string;
  certificateNoMasked?: string;
  lastLoginAt?: string;
  status?: string;
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
