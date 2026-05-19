import {useAuthStore} from "~/stores/authStore";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";

export default defineNuxtRouteMiddleware(async (to, from) => {
  if (to.path === '/auth') return;

  const authStore = useAuthStore();

  if (import.meta.client && from.path && authStore.isAuthenticated) return;

  const authService = useAuthService(useApi());
  // Capture useToasts() before the await: Nuxt's async context is lost across awaits, so calling
  // it from the catch branch would warn ("composable called outside setup").
  const toasts = import.meta.client ? useToasts() : null;

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
    toasts?.error("Verification failed", "Could not verify session with the server.");
  }
})
