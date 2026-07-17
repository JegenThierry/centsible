import {getActivePinia, type Pinia} from 'pinia'
import {invalidateLedgerAggregates} from '~/utils/ledger-aggregates'

interface ResettableStore {
  $id: string
  $reset?: () => void
}

/**
 * Resets every instantiated Pinia store to its initial state and drops the ledger-aggregate memos.
 *
 * Login and logout are pure SPA navigation — the tab never reloads — so without this every feature
 * store (categories, budgets, loans, accounts, saved filters, …) keeps the previous session's data,
 * and the `length === 0` / load-once guards then skip the refetch and hand it to the next user. Call
 * on every sign-out path: {@link useAuthStore}'s logout and account deletion, and the axios 401
 * interceptor.
 *
 * `authStore` is skipped — it owns the sign-out and reassigns its own cookie-backed flags — and
 * un-instantiated stores are absent from `_s`, so a store never touched this session holds no state
 * to leak and is nothing to reset.
 */
export function resetAllStores(): void {
  const pinia = getActivePinia() as Pinia | undefined
  if (!pinia) return
  const stores = (pinia as unknown as {_s: Map<string, ResettableStore>})._s
  stores.forEach((store) => {
    if (store.$id === 'authStore') return
    store.$reset?.()
  })
  invalidateLedgerAggregates()
}
