import {useAuthStore} from "~/stores/authStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useOnboarded} from "~/composables/use-onboarded";

export default defineNuxtRouteMiddleware(async () => {
  const nuxtApp = useNuxtApp();

  if (import.meta.client && nuxtApp.isHydrating && nuxtApp.payload.serverRendered) return;

  const onboarded = import.meta.client ? useOnboarded() : undefined;
  if (onboarded?.value) return;

  const accountsStore = useBudgetAccountsStore();
  const loaded = accountsStore.availableAccounts.length > 0
    || await accountsStore.updateAvailableAccounts();

  if (!useAuthStore().isAuthenticated) {
    return await nuxtApp.runWithContext(() => navigateTo('/auth'));
  }

  if (!loaded) return;

  if (accountsStore.availableAccounts.length === 0) {
    return await nuxtApp.runWithContext(() => navigateTo('/onboarding'));
  }

  if (onboarded) {
    onboarded.value = true;
  }
});
