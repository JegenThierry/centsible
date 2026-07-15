import {defineStore} from 'pinia'
import adze from 'adze'
import {useUserStore} from "~/stores/userStore";
import {useAuthService} from "~/services/auth/auth-service";

export const useAuthStore = defineStore('authStore', () => {
  const api = useApi();
  const authService = useAuthService(api);

  const isAuthenticated = ref(false)

  /**
   * Cookie-backed so the /auth/2fa guard can read it during SSR; an in-memory ref is always false on
   * the server, which bounced a reloading user out of a challenge they were mid-way through. This is a
   * non-httpOnly hint that grants no authority — the real gate stays the httpOnly `pre_auth` cookie the
   * backend verifies. maxAge mirrors PENDING_TTL in AuthCookieSupport.kt so the hint cannot outlive the
   * challenge it describes.
   */
  const twoFactorPending = useCookie<boolean>('centsible_2fa_pending', {
    maxAge: 300,
    sameSite: 'strict',
    path: '/',
    default: () => false,
  })

  const setAuthenticated = (value: boolean) => {
    isAuthenticated.value = value
  }

  const setTwoFactorPending = (value: boolean) => {
    twoFactorPending.value = value
  }

  const logout = () => {
    isAuthenticated.value = false
    twoFactorPending.value = false
    useUserStore().clear();
    authService.logout().catch((error) => {
      adze.ns('auth').warn('Server logout failed; cookie may still be valid until it expires.', error)
    })
    return navigateTo('/auth')
  }

  const changePassword = (currentPassword: string, newPassword: string) =>
    authService.changePassword({currentPassword, newPassword})

  const signOutEverywhere = () => authService.signOutEverywhere()

  const deleteAccount = async (password: string, totpCode?: string) => {
    await authService.deleteAccount({password, totpCode})
    isAuthenticated.value = false
    twoFactorPending.value = false
    useUserStore().clear()
    await navigateTo('/auth')
  }

  return {
    isAuthenticated, twoFactorPending, setAuthenticated, setTwoFactorPending, logout,
    changePassword, signOutEverywhere, deleteAccount,
  }
});
