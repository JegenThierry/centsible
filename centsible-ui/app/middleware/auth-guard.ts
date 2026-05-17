import {useAuthStore} from "~/stores/authStore";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";

export default defineNuxtRouteMiddleware(async (to, from) => {
  if (to.path === '/auth') return;

  const authStore = useAuthStore();

  if (import.meta.client && from.path && authStore.isAuthenticated) return;

  const authService = useAuthService(useApi());

  try {
    const isVerified = await authService.verify();
    authStore.setAuthenticated(isVerified);
    if (!isVerified) return navigateTo('/auth');
  } catch (error) {
    const status = (error as {response?: {status?: number}})?.response?.status;
    if (status === 401 || status === 403) {
      authStore.setAuthenticated(false);
      return navigateTo('/auth');
    }
    if (import.meta.client) {
      useToasts().error("Verification failed", "Could not verify session with the server.");
    }
  }
})
