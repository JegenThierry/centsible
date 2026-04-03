import {defineStore} from 'pinia'
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {Transaction} from "~/models/transactions/transaction";

export const useTransactionStore = defineStore('transactionStore', () => {
  const transactionService = useTransactionService(useApi());
  const transactions = ref<Transaction[]>([]);

  async function fetchTransactions(accountId: string) {
    try {
      transactions.value = await transactionService.fetchTransactions(accountId, 1, 100);
    } catch (error) {
      console.error("Failed to fetch transactions", error);
    }
  }

  return {
    transactions,
    fetchTransactions
  }
});
