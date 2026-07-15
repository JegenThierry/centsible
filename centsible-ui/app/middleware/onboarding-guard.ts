import {useAuthStore} from "~/stores/authStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useOnboarded} from "~/composables/use-onboarded";

export default defineNuxtRouteMiddleware(async () => {
  const nuxtApp = useNuxtApp();

  // The server already ran this guard for the initial route; re-running it on hydration would only
  // repeat the fetch and re-decide what has already been decided.
  if (import.meta.client && nuxtApp.isHydrating && nuxtApp.payload.serverRendered) return;

  // localStorage-backed, so it only carries a real value on the client. On the server it is always
  // the `false` default and must not short-circuit.
  const onboarded = import.meta.client ? useOnboarded() : undefined;
  if (onboarded?.value) return;

  const accountsStore = useBudgetAccountsStore();
  const loaded = accountsStore.availableAccounts.length > 0
    || await accountsStore.updateAvailableAccounts();

  // The axios interceptor cannot redirect from inside middleware, so own the sign-out here — without
  // this, an expired session falls through to the onboarding branch below.
  if (!useAuthStore().isAuthenticated) {
    return await nuxtApp.runWithContext(() => navigateTo('/auth'));
  }

  // An empty list after a failed fetch means "unknown", not "no accounts". Fail open and let /accounts
  // render its own error state rather than dumping an established user into first-run onboarding.
  if (!loaded) return;

  if (accountsStore.availableAccounts.length === 0) {
    return await nuxtApp.runWithContext(() => navigateTo('/onboarding'));
  }

  if (onboarded) {
    onboarded.value = true;
  }
});
