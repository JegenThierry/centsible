import {ref, type Ref, watch} from 'vue'
import type {Transaction} from '~/models/transactions/transaction'
import type {TransactionFilters} from '~/models/transactions/transaction-filters'
import type {useBudgetAccountsStore} from '~/stores/budgetAccountsStore'
import type {useTransactionService} from '~/services/transactions/transaction-service'

type TransactionService = ReturnType<typeof useTransactionService>
type BudgetAccountsStore = ReturnType<typeof useBudgetAccountsStore>

export function useTransactionList(
  transactionService: TransactionService,
  budgetAccountsStore: BudgetAccountsStore,
  pageSizeValue = 25,
  filters?: Ref<TransactionFilters>,
) {
  const transactions = ref<Transaction[]>([])
  const page = ref(1)
  const pageSize = ref(pageSizeValue)
  const loading = ref(false)
  const loadingMore = ref(false)
  const hasMore = ref(true)

  async function loadTransactions(reset = false) {
    if (!budgetAccountsStore.activeAccount?.id) return

    if (reset) {
      page.value = 1
      hasMore.value = true
      transactions.value = []
      loading.value = true
    } else {
      loadingMore.value = true
    }

    try {
      const data = await transactionService.fetchTransactions(
        budgetAccountsStore.activeAccount.id,
        page.value,
        pageSize.value,
        filters?.value ?? {},
      )

      if (data.length < pageSize.value) {
        hasMore.value = false
      }

      transactions.value = [...transactions.value, ...data]
      page.value++
    } catch (error) {
      console.error('Failed to fetch transactions:', error)
    } finally {
      loading.value = false
      loadingMore.value = false
    }
  }

  if (filters) {
    watch(
      filters,
      () => {
        if (budgetAccountsStore.activeAccount?.id) loadTransactions(true)
      },
      {deep: true},
    )
  }

  return {
    transactions,
    loading,
    loadingMore,
    hasMore,
    loadTransactions
  }
}
