import adze from 'adze';
import {useAuthStore} from "~/stores/authStore";
import {useAuthService} from "~/services/auth/auth-service";

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
    adze.ns('auth').error('Session verification failed', error);
    // Fail closed: rendering a protected page with an unverified session produces inconsistent
    // authed/unauthed states. A retryable error page is also visible on the SSR path, where a
    // toast is not.
    throw createError({statusCode: 503, statusMessage: 'sessionUnavailable', fatal: true});
  }
})
