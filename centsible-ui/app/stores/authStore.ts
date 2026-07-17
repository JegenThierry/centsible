import {defineStore} from 'pinia'
import adze from 'adze'
import {useUserStore} from "~/stores/userStore";
import {useAuthService} from "~/services/auth/auth-service";

export const useAuthStore = defineStore('authStore', () => {
  const api = useApi();
  const authService = useAuthService(api);

  const isAuthenticated = ref(false)

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
    resetAllStores();
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
    resetAllStores()
    await navigateTo('/auth')
  }

  return {
    isAuthenticated, twoFactorPending, setAuthenticated, setTwoFactorPending, logout,
    changePassword, signOutEverywhere, deleteAccount,
  }
});
