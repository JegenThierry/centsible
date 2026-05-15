import {defineStore} from 'pinia'
import {useUserStore} from "~/stores/userStore";
import {useAuthService} from "~/services/auth/auth-service";

// JWT lives in the HttpOnly cookie; auth-guard restores this flag via /auth/verify.
export const useAuthStore = defineStore('authStore', () => {
  const api = useApi();
  const authService = useAuthService(api);

  const isAuthenticated = ref(false)

  const setAuthenticated = (value: boolean) => {
    isAuthenticated.value = value
  }

  const logout = () => {
    // Fire-and-forget the server call: a failed roundtrip leaves the cookie alive,
    // but the auth-guard's next /verify would just re-authenticate the user anyway,
    // so making the user wait gains us nothing.
    isAuthenticated.value = false
    useUserStore().clear();
    authService.logout().catch(() => {})
    return navigateTo('/auth')
  }

  return {
    isAuthenticated, setAuthenticated, logout,
  }
});
