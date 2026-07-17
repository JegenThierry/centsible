import {useAuthStore} from "~/stores/authStore";

/** Keeps already-authenticated users out of the guest-only auth pages by bouncing them to /accounts. */
export default defineNuxtRouteMiddleware(() => {
  const authStore = useAuthStore();
  if (authStore.isAuthenticated) return navigateTo('/accounts');
});
