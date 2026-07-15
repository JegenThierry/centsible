import {defineStore} from "pinia";
import adze from 'adze'
import type {BudgetAccount, UpdateBudgetAccountForm} from "~/models/budget-account/budget-account";
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import {useApiErrors} from "~/composables/use-api-errors";

export const useBudgetAccountsStore = defineStore('budgetAccountsStore', () => {
  const api = useApi();
  const apiErrors = useApiErrors();
  const {t} = useNuxtApp().$i18n;
  const accountService = useBudgetAccountService(api);

  const activeAccount = ref<BudgetAccount>();
  const availableAccounts = ref<BudgetAccount[]>([]);
  const pending = ref(false);

  /** Returns whether the list was actually refreshed, so callers can tell an empty list from a failed fetch. */
  async function updateAvailableAccounts(): Promise<boolean> {
    pending.value = true;
    try {
      availableAccounts.value = await accountService.fetchAccounts();
      return true;
    } catch (error) {
      apiErrors.toastError(error, t('accounts.toasts.updateFailedTitle'), t('accounts.toasts.updateFailedBody'));
      return false;
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
      adze.ns('budget-accounts').error('Failed to update active account', error);
    } finally {
      pending.value = false;
    }
  }

  /**
   * Rejects instead of toasting: account-loader has to tell a missing account (redirect to the list)
   * apart from a transient failure (keep the user where they are), which a swallowed error hides.
   */
  async function loadActiveAccount(accountId: string) {
    if (activeAccount.value?.id === accountId) return;

    pending.value = true;
    try {
      activeAccount.value = await accountService.fetchAccount(accountId);
    } finally {
      pending.value = false;
    }
  }

  function clearActiveAccount() {
    activeAccount.value = undefined;
  }

  async function updateAccount(id: string, form: UpdateBudgetAccountForm): Promise<BudgetAccount> {
    pending.value = true;
    try {
      const updated = await accountService.updateAccount(id, form);
      availableAccounts.value = availableAccounts.value.map((account) => account.id === id ? updated : account);
      if (activeAccount.value?.id === id) activeAccount.value = updated;
      return updated;
    } finally {
      pending.value = false;
    }
  }

  async function deleteAccount(id: string): Promise<void> {
    pending.value = true;
    try {
      await accountService.deleteAccount(id);
      availableAccounts.value = availableAccounts.value.filter((account) => account.id !== id);
      if (activeAccount.value?.id === id) clearActiveAccount();
    } finally {
      pending.value = false;
    }
  }

  return {
    activeAccount,
    availableAccounts,
    pending,
    updateAvailableAccounts,
    updateActiveAccount,
    loadActiveAccount,
    clearActiveAccount,
    updateAccount,
    deleteAccount,
  }
});
