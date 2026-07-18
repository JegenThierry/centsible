import {defineStore} from 'pinia';
import adze from 'adze'
import type {RecurringTransaction} from "~/models/recurring/recurring-transaction";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";
import {createLatestRequestGate} from "~/utils/latest-request";

export const useRecurringTransactionsStore = defineStore('recurringTransactionsStore', () => {
  const service = useRecurringTransactionService(useApi());
  const items = ref<RecurringTransaction[]>([]);
  const loading = ref(false);
  const error = ref(false);

  const gate = createLatestRequestGate();

  async function fetchForAccount(accountId: string) {
    const isLatest = gate.begin();
    loading.value = true;
    error.value = false;
    try {
      const fetched = await service.fetchAll(accountId);
      if (isLatest()) items.value = fetched;
    } catch (e) {
      adze.ns('recurring').error('Failed to fetch recurring transactions', e);
      if (isLatest()) {
        items.value = [];
        error.value = true;
      }
    } finally {
      if (isLatest()) loading.value = false;
    }
  }

  function reset() {
    gate.supersede();
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
