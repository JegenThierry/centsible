import {useAuthStore} from "~/stores/authStore";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";

export default defineNuxtRouteMiddleware(async (to) => {
  const authStore = useAuthStore();
  const api = useApi();
  const authService = useAuthService(api);
  const toasts = useToasts();

  if (to.path === '/auth') {
    return;
  }

  if (!authStore.isAuthenticated) {
    return navigateTo('/auth');
  }

  try {
    const isVerified = await authService.verify();

    if (!isVerified) {
      return navigateTo('/auth');
    }

  } catch (error) {
    console.error(error);
    toasts.error("Failed to verify user", "Your session may have expired. Please log in again.");
    return authStore.logout();
  }
})
