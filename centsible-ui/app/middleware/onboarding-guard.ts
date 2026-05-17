import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

const SKIP_PATHS = ['/onboarding', '/auth', '/profile'];

export default defineNuxtRouteMiddleware(async (to) => {
  if (!import.meta.client) return;
  if (SKIP_PATHS.some((p) => to.path.startsWith(p))) return;

  if (localStorage.getItem('centsible.onboarded') === '1') return;

  const nuxtApp = useNuxtApp();
  const accountsStore = useBudgetAccountsStore();
  if (accountsStore.availableAccounts.length === 0) {
    await accountsStore.updateAvailableAccounts();
  }

  if (accountsStore.availableAccounts.length === 0) {
    return await nuxtApp.runWithContext(() => navigateTo('/onboarding'));
  }

  localStorage.setItem('centsible.onboarded', '1');
});
