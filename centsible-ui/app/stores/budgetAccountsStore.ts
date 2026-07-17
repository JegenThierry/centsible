import {defineStore} from "pinia";
import adze from 'adze'
import type {BudgetAccount, UpdateBudgetAccountForm} from "~/models/budget-account/budget-account";
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {createLatestRequestGate} from "~/utils/latest-request";

export const useBudgetAccountsStore = defineStore('budgetAccountsStore', () => {
  const api = useApi();
  const apiErrors = useApiErrors();
  const {t} = useNuxtApp().$i18n;
  const accountService = useBudgetAccountService(api);

  const activeAccount = ref<BudgetAccount>();
  const availableAccounts = ref<BudgetAccount[]>([]);
  const pending = ref(false);

  /**
   * Guards every write to `activeAccount`. Navigating to a slow account A then quickly to a fast
   * account B used to leave A's late response sitting on B's page — the header showed A's balance
   * and the transaction-list watcher paged A's rows into B's list. One gate, not one per action:
   * they all race for the same ref.
   */
  const activeAccountGate = createLatestRequestGate();

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
    const current = activeAccount.value;
    if (!current) return;

    const isLatest = activeAccountGate.begin();
    pending.value = true;
    try {
      const fetched = await accountService.fetchAccount(current.id);
      if (isLatest()) activeAccount.value = fetched;
    } catch (error) {
      adze.ns('budget-accounts').error('Failed to update active account', error);
    } finally {
      // A superseded refresh leaves `pending` to whichever request outran it.
      if (isLatest()) pending.value = false;
    }
  }

  /**
   * Rejects instead of toasting: account-loader has to tell a missing account (redirect to the list)
   * apart from a transient failure (keep the user where they are), which a swallowed error hides.
   */
  async function loadActiveAccount(accountId: string) {
    if (activeAccount.value?.id === accountId) return;

    const isLatest = activeAccountGate.begin();
    pending.value = true;
    try {
      const fetched = await accountService.fetchAccount(accountId);
      if (isLatest()) activeAccount.value = fetched;
    } finally {
      if (isLatest()) pending.value = false;
    }
  }

  function clearActiveAccount() {
    // Supersede first: an in-flight fetch would otherwise resurrect the account we just cleared.
    activeAccountGate.supersede();
    activeAccount.value = undefined;
  }

  async function updateAccount(id: string, form: UpdateBudgetAccountForm): Promise<BudgetAccount> {
    pending.value = true;
    try {
      const updated = await accountService.updateAccount(id, form);
      availableAccounts.value = availableAccounts.value.map((account) => account.id === id ? updated : account);
      if (activeAccount.value?.id === id) {
        // Freshest truth for this account — supersede any in-flight fetch so an older response
        // can't land afterwards and roll the edit back.
        activeAccountGate.supersede();
        activeAccount.value = updated;
      }
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
