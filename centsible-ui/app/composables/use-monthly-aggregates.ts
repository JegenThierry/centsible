import {ref, watch} from 'vue';
import adze from 'adze'
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {MonthlyAggregate} from "~/models/transactions/transaction";
import {createAsyncCache} from "~/utils/async-cache";
import {registerLedgerAggregateCache} from "~/utils/ledger-aggregates";

type TransactionService = ReturnType<typeof useTransactionService>;

const cache = registerLedgerAggregateCache(createAsyncCache<MonthlyAggregate[]>(() => []));

function key(accountId: string, months: number): string {
  return `${accountId}|${months}`;
}

/**
 * Warm the cache from a parent (e.g. the dashboard) so children mounting later hit a resolved
 * entry instead of kicking off a second wave of network calls.
 */
export async function prefetchMonthlyAggregates(
  service: TransactionService,
  accountId: string,
  months: number,
): Promise<void> {
  if (!accountId) return;
  try {
    await cache.loadOrCache(key(accountId, months), () => service.aggregateByMonth(accountId, months));
  } catch (e) {
    adze.ns('dashboard').error('Failed to prefetch monthly aggregates', e);
  }
}

export function useMonthlyAggregates(accountId: () => string, months: () => number) {
  const service = useTransactionService(useApi());
  const data = ref<MonthlyAggregate[]>([]);
  const loading = ref(false);
  const error = ref(false);
  let token = 0;

  async function load() {
    const current = ++token;
    const id = accountId();
    const m = months();
    if (!id) {
      data.value = [];
      return;
    }
    loading.value = true;
    error.value = false;
    try {
      const result = await cache.loadOrCache(key(id, m), () => service.aggregateByMonth(id, m));
      if (current === token) data.value = result;
    } catch (e) {
      adze.ns('dashboard').error('Failed to load monthly aggregates', e);
      if (current === token) {
        data.value = [];
        error.value = true;
      }
    } finally {
      if (current === token) loading.value = false;
    }
  }

  watch([accountId, months], load, {immediate: true});

  return {data, loading, error, reload: load};
}
