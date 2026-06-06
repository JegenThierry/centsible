import {defineStore} from 'pinia'
import adze from 'adze'
import {useUserStore} from "~/stores/userStore";
import {useAuthService} from "~/services/auth/auth-service";

export const useAuthStore = defineStore('authStore', () => {
  const api = useApi();
  const authService = useAuthService(api);

  const isAuthenticated = ref(false)

  const setAuthenticated = (value: boolean) => {
    isAuthenticated.value = value
  }

  const logout = () => {
    isAuthenticated.value = false
    useUserStore().clear();
    authService.logout().catch((error) => {
      adze.ns('auth').warn('Server logout failed; cookie may still be valid until it expires.', error)
    })
    return navigateTo('/auth')
  }

  return {
    isAuthenticated, setAuthenticated, logout,
  }
});
