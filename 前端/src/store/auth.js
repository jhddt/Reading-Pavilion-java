import { defineStore } from 'pinia';

const TOKEN_KEY = 'rp_token';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    username: '',
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    authHeader: (state) => (state.token ? `Bearer ${state.token}` : ''),
  },
  actions: {
    setToken(token) {
      this.token = token;
      if (token) {
        localStorage.setItem(TOKEN_KEY, token);
      } else {
        localStorage.removeItem(TOKEN_KEY);
      }
    },
    setUsername(name) {
      this.username = name || '';
    },
    logout() {
      this.setToken('');
      this.setUsername('');
    },
  },
});

