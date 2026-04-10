import {defineStore} from "pinia";
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import {useToasts} from "~/services/toasts/toast-service";

export const useBudgetAccountsStore = defineStore('budgetAccountsStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const accountService = useBudgetAccountService(api);

  const activeAccount = ref<BudgetAccount>();
  const availableAccounts = ref<BudgetAccount[]>([]);
  const pending = ref(false);

  async function updateAvailableAccounts() {
    const accountService = useBudgetAccountService(api);
    pending.value = true;
    try {
      availableAccounts.value = await accountService.fetchAccounts();
    } catch (error) {
      toasts.error("Failed to update accounts", "Accounts could not be updated")
      console.error(error);
    } finally {
      pending.value = false;
    }
  }

  async function updateActiveAccount() {
    if (activeAccount.value == undefined) return;

    pending.value = true;
    try {
      activeAccount.value = await accountService.fetchAccount(activeAccount.value.id)
    } catch (error) {
      console.error(error);
    } finally {
      pending.value = false;
    }
  }

  async function loadActiveAccount(accountId: string) {
    if (activeAccount.value?.id === accountId) {
      return;
    }

    pending.value = true;
    try {
      activeAccount.value = await accountService.fetchAccount(accountId);
    } catch (error) {
      toasts.error("Failed to load account", "Account could not be loaded");
      console.error(error);
    } finally {
      pending.value = false;
    }
  }

  function clearActiveAccount() {
    activeAccount.value = undefined;
  }

  return {
    activeAccount,
    availableAccounts,
    pending,
    updateAvailableAccounts,
    updateActiveAccount,
    loadActiveAccount,
    clearActiveAccount,
  }
});
