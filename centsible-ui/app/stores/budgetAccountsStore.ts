import {defineStore} from "pinia";
import adze from 'adze'
import type {BudgetAccount} from "~/models/budget-account/budget-account";
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

  async function updateAvailableAccounts() {
    pending.value = true;
    try {
      availableAccounts.value = await accountService.fetchAccounts();
    } catch (error) {
      apiErrors.toastError(error, t('accounts.toasts.updateFailedTitle'), t('accounts.toasts.updateFailedBody'));
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

  async function loadActiveAccount(accountId: string) {
    if (activeAccount.value?.id === accountId) return;

    pending.value = true;
    try {
      activeAccount.value = await accountService.fetchAccount(accountId);
    } catch (error) {
      apiErrors.toastError(error, t('accounts.toasts.loadFailedTitle'), t('accounts.toasts.loadFailedBody'));
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
