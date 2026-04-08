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
      return authStore.logout();
    }

  } catch (error: any) {
    console.error('Auth verification error:', error);

    // If it's a 401 or 403, the token is definitely invalid, so log out.
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      return authStore.logout();
    }

    // If it's a connection error or other server-side issue, don't log out immediately.
    // This prevents deleting the token if the backend is temporarily unreachable during SSR.
    if (import.meta.client) {
      toasts.error("Verification failed", "Could not verify session with the server.");
    }
  }
})
