import {defineStore} from "pinia";
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import {useApiErrors} from "~/composables/use-api-errors";

export const useBudgetAccountsStore = defineStore('budgetAccountsStore', () => {
  const api = useApi();
  const apiErrors = useApiErrors();
  const accountService = useBudgetAccountService(api);

  const activeAccount = ref<BudgetAccount>();
  const availableAccounts = ref<BudgetAccount[]>([]);
  const pending = ref(false);

  async function updateAvailableAccounts() {
    pending.value = true;
    try {
      availableAccounts.value = await accountService.fetchAccounts();
    } catch (error) {
      apiErrors.toastError(error, "Failed to update accounts", "Accounts could not be updated");
    } finally {
      pending.value = false;
    }
  }

  async function updateActiveAccount() {
    if (!activeAccount.value) return;

    pending.value = true;
    try {
      activeAccount.value = await accountService.fetchAccount(activeAccount.value.id);
    } catch (error) {
      console.error(error);
    } finally {
      pending.value = false;
    }
  }

  async function loadActiveAccount(accountId: string) {
    if (activeAccount.value?.id === accountId) return;

    pending.value = true;
    try {
      activeAccount.value = await accountService.fetchAccount(accountId);
    } catch (error) {
      apiErrors.toastError(error, "Failed to load account", "Account could not be loaded");
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
