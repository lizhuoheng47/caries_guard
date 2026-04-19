import { defineStore } from 'pinia';
import { authApi } from '../api/auth';
import { AuthAdapter } from '../api/adapters/auth';
import type { User, UserPermissions } from '../models/auth';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null as User | null,
    permissions: null as UserPermissions | null,
  }),
  
  getters: {
    isAuthenticated: (state) => !!state.token,
    menus: (state) => state.permissions?.menus || [],
  },
  
  actions: {
    async login(credentials: any) {
      const res = await authApi.login(credentials);
      const authTokens = AuthAdapter.toAuthTokens(res.data);
      this.token = authTokens.accessToken;
      localStorage.setItem('token', this.token);
      this.user = res.data.user ? AuthAdapter.toUser(res.data.user) : this.user;
      await this.fetchUserInfo({ requireUserRefresh: !res.data.user });
    },
    
    async fetchUserInfo(options?: { requireUserRefresh?: boolean; logoutOnFailure?: boolean }) {
      if (!this.token) return;

      const requireUserRefresh = options?.requireUserRefresh ?? true;
      const logoutOnFailure = options?.logoutOnFailure ?? true;

      try {
        if (requireUserRefresh || !this.user) {
          const userRes = await authApi.getMe();
          this.user = AuthAdapter.toUser(userRes.data);
        }
      } catch (error) {
        if (logoutOnFailure) {
          this.logout();
        }
        throw error;
      }

      try {
        const permRes = await authApi.getPermissions();
        this.permissions = AuthAdapter.toUserPermissions(permRes.data);
      } catch {
        this.permissions = { codes: [], menus: [] };
      }
    },
    
    logout() {
      this.token = '';
      this.user = null;
      this.permissions = null;
      localStorage.removeItem('token');
    }
  }
});
