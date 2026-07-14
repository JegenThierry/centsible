import {useSetupService} from "~/services/setup/setup-service";

/** Sends a fresh instance to /setup, and blocks re-running setup once any user exists. */
export default defineNuxtRouteMiddleware(async (to) => {
  let needsSetup = false;
  try {
    needsSetup = await useSetupService(useApi()).status();
  } catch {
    return; // status unreachable — don't trap the user on this page
  }
  if (needsSetup && to.path !== '/setup') return navigateTo('/setup');
  if (!needsSetup && to.path === '/setup') return navigateTo('/auth');
});
