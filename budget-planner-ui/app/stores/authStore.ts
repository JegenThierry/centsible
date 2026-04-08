import {defineStore} from 'pinia'

const ONE_WEEK = 60 * 60 * 24 * 7;

export const useAuthStore = defineStore('authStore', () => {
  const token = useCookie<string | null>('auth_token', {
    maxAge: ONE_WEEK, watch: true, path: '/'
  })

  const isAuthenticated = computed(() => !!token.value)

  const setToken = (newToken: string) => {
    token.value = newToken
  }

  const logout = () => {
    token.value = null;
    return navigateTo('/auth')
  }

  return {
    isAuthenticated, token, logout, setToken,
  }
});
