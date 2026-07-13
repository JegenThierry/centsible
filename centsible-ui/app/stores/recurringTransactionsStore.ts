import {defineStore} from 'pinia';
import adze from 'adze'
import type {RecurringTransaction} from "~/models/recurring/recurring-transaction";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";

export const useRecurringTransactionsStore = defineStore('recurringTransactionsStore', () => {
  const service = useRecurringTransactionService(useApi());
  const items = ref<RecurringTransaction[]>([]);
  const loading = ref(false);
  const error = ref(false);

  async function fetchForAccount(accountId: string) {
    loading.value = true;
    error.value = false;
    try {
      items.value = await service.fetchAll(accountId);
    } catch (e) {
      adze.ns('recurring').error('Failed to fetch recurring transactions', e);
      items.value = [];
      error.value = true;
    } finally {
      loading.value = false;
    }
  }

  function reset() {
    items.value = [];
    error.value = false;
  }

  return {
    items,
    loading,
    error,
    fetchForAccount,
    reset,
  };
});
