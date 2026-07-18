import {ref, type Ref} from 'vue'
import {watchDeep} from '@vueuse/core'
import adze from 'adze'
import type {Transaction} from '~/models/transactions/transaction'
import type {TransactionFilters} from '~/models/transactions/transaction-filters'
import type {useBudgetAccountsStore} from '~/stores/budgetAccountsStore'
import type {useTransactionService} from '~/services/transactions/transaction-service'

type TransactionService = ReturnType<typeof useTransactionService>
type BudgetAccountsStore = ReturnType<typeof useBudgetAccountsStore>

/** Paginated transaction list for the active account; appends pages and reloads on [filters] change. */
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
  const error = ref(false)
  let requestToken = 0

  async function loadTransactions(reset = false) {
    if (!budgetAccountsStore.activeAccount?.id) return

    const current = ++requestToken
    error.value = false
    if (reset) {
      hasMore.value = true
      loading.value = true
    } else {
      loadingMore.value = true
    }

    const requestedPage = reset ? 1 : page.value

    try {
      const data = await transactionService.fetchTransactions(
        budgetAccountsStore.activeAccount.id,
        requestedPage,
        pageSize.value,
        filters?.value ?? {},
      )
      if (current !== requestToken) return

      hasMore.value = data.length >= pageSize.value
      transactions.value = reset ? data : [...transactions.value, ...data]
      page.value = requestedPage + 1
    } catch (err) {
      if (current !== requestToken) return
      error.value = true
      adze.ns('transactions').error('Failed to fetch transactions', err)
    } finally {
      if (current === requestToken) {
        loading.value = false
        loadingMore.value = false
      }
    }
  }

  if (filters) {
    watchDeep(filters, () => {
      if (budgetAccountsStore.activeAccount?.id) loadTransactions(true)
    })
  }

  return {
    transactions,
    loading,
    loadingMore,
    hasMore,
    error,
    loadTransactions
  }
}
