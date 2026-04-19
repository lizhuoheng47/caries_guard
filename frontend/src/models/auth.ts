export interface AuthTokens {
  accessToken: string;
  refreshToken?: string;
  expiresAt?: string;
}

export interface User {
  id: number;
  username: string;
  nickname: string;
  roles: string[];
  orgId: number;
}

export interface MenuItem {
  id: number;
  parentId: number;
  name: string;
  path: string;
  icon?: string;
  children?: MenuItem[];
}

export interface UserPermissions {
  codes: string[];
  menus: MenuItem[];
}
