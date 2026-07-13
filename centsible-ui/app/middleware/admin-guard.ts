import {useUserStore} from "~/stores/userStore";

/**
 * Requires the current user to be the instance admin (runs after auth-guard). The flag comes from
 * /users/myself; when the feature is disabled server-side nobody carries it, so no separate
 * feature-flag check is needed. The backend re-checks every /admin call regardless.
 */
export default defineNuxtRouteMiddleware(async () => {
  const userStore = useUserStore();

  if (!userStore.user) {
    try {
      await userStore.fetchMyself();
    } catch {
      return navigateTo('/accounts');
    }
  }

  if (!userStore.user?.admin) return navigateTo('/accounts');
})
