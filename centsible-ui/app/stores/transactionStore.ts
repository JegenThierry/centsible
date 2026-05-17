import {defineStore} from 'pinia'
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {Transaction} from "~/models/transactions/transaction";

export const useTransactionStore = defineStore('transactionStore', () => {
  const transactionService = useTransactionService(useApi());
  const transactions = ref<Transaction[]>([]);
  const pending = ref(false);

  async function fetchTransactions(accountId: string, size: number = 25) {
    pending.value = true;
    try {
      transactions.value = await transactionService.fetchTransactions(accountId, 1, size);
    } catch (error) {
      console.error("Failed to fetch transactions", error);
    } finally {
      pending.value = false;
    }
  }

  return {
    transactions,
    pending,
    fetchTransactions
  }
});
