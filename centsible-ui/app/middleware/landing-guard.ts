import adze from 'adze';
import {useAuthStore} from "~/stores/authStore";
import {useAuthService} from "~/services/auth/auth-service";

/**
 * Landing-page guard: sends an already-verified session straight to /accounts, otherwise leaves the
 * visitor on the public landing page. Verification failures are non-fatal here (unlike auth-guard) —
 * an anonymous visitor is expected.
 */
export default defineNuxtRouteMiddleware(async () => {
  const nuxtApp = useNuxtApp();
  const authStore = useAuthStore();
  const api = useApi();
  const authService = useAuthService(api);

  if (import.meta.client && authStore.isAuthenticated) {
    return navigateTo('/accounts');
  }

  try {
    const isVerified = await authService.verify();
    if (isVerified) {
      authStore.setAuthenticated(true);
      return await nuxtApp.runWithContext(() => navigateTo('/accounts'));
    }
  } catch (error) {
    adze.ns('auth').warn('Session verification failed; staying on landing', error);
  }
});
