export default defineNuxtRouteMiddleware(async (to, _) => {
  const accountStore = useBudgetAccountsStore();
  const accountId = to.params.accountId as string;

  if (!accountId) {
    return;
  }

  if (accountStore.activeAccount?.id !== accountId) {
    accountStore.clearActiveAccount();
  }

  await accountStore.loadActiveAccount(accountId);

  if (!accountStore.activeAccount) {
    return navigateTo('/accounts');
  }
})
