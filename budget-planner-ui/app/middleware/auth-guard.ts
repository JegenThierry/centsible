import {useAuthStore} from "~/stores/authStore";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";

export default defineNuxtRouteMiddleware(async (to, from) => {
  const authStore = useAuthStore();
  const api = useApi();
  const authService = useAuthService(api);
  const toasts = useToasts();

  if (to.path === '/auth') {
    return;
  }

  // Client-side in-app navigation when we already know the session is valid: trust the flag.
  // Stale sessions surface as 401s on the next data call. SSR + first load still verify.
  if (import.meta.client && from.path && authStore.isAuthenticated) {
    return;
  }

  try {
    const isVerified = await authService.verify();

    if (!isVerified) {
      authStore.setAuthenticated(false);
      return navigateTo('/auth');
    }
    authStore.setAuthenticated(true);
  } catch (error: any) {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      authStore.setAuthenticated(false);
      return navigateTo('/auth');
    }

    // Network / 5xx: toast and stay; don't log the user out for a transient outage.
    if (import.meta.client) {
      toasts.error("Verification failed", "Could not verify session with the server.");
    }
  }
})
