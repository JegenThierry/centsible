import {defineStore} from 'pinia';
import type {RecurringTransaction} from "~/models/recurring/recurring-transaction";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";

export const useRecurringTransactionsStore = defineStore('recurringTransactionsStore', () => {
  const service = useRecurringTransactionService(useApi());
  const items = ref<RecurringTransaction[]>([]);
  const loading = ref(false);

  async function fetchForAccount(accountId: string) {
    loading.value = true;
    try {
      items.value = await service.fetchAll(accountId);
    } catch (error) {
      console.error('Failed to fetch recurring transactions', error);
      items.value = [];
    } finally {
      loading.value = false;
    }
  }

  function reset() {
    items.value = [];
  }

  return {
    items,
    loading,
    fetchForAccount,
    reset,
  };
});
