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
      
      await this.fetchUserInfo();
    },
    
    async fetchUserInfo() {
      if (!this.token) return;
      
      try {
        const [userRes, permRes] = await Promise.all([
          authApi.getMe(),
          authApi.getPermissions()
        ]);
        
        this.user = AuthAdapter.toUser(userRes.data);
        this.permissions = AuthAdapter.toUserPermissions(permRes.data);
      } catch (error) {
        this.logout();
        throw error;
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
