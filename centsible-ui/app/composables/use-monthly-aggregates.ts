import {ref, watch} from 'vue';
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {MonthlyAggregate} from "~/models/transactions/transaction";

type TransactionService = ReturnType<typeof useTransactionService>;

interface CacheEntry {
  data: MonthlyAggregate[];
  promise: Promise<MonthlyAggregate[]> | null;
}

const cache = new Map<string, CacheEntry>();

function key(accountId: string, months: number): string {
  return `${accountId}|${months}`;
}

async function loadOrCache(
  service: TransactionService,
  accountId: string,
  months: number,
): Promise<MonthlyAggregate[]> {
  const k = key(accountId, months);
  const entry = cache.get(k);
  if (entry) {
    if (entry.promise) return entry.promise;
    return entry.data;
  }
  const promise = service.aggregateByMonth(accountId, months);
  cache.set(k, {data: [], promise});
  try {
    const result = await promise;
    cache.set(k, {data: result, promise: null});
    return result;
  } catch (e) {
    cache.delete(k);
    throw e;
  }
}

// Warm the cache from a parent (e.g. the dashboard) so children mounting later hit a resolved
// entry instead of kicking off a second wave of network calls.
export async function prefetchMonthlyAggregates(
  service: TransactionService,
  accountId: string,
  months: number,
): Promise<void> {
  if (!accountId) return;
  try {
    await loadOrCache(service, accountId, months);
  } catch (e) {
    console.error('Failed to prefetch monthly aggregates', e);
  }
}

export function useMonthlyAggregates(accountId: () => string, months: () => number) {
  const service = useTransactionService(useApi());
  const data = ref<MonthlyAggregate[]>([]);
  const loading = ref(false);

  async function load() {
    const id = accountId();
    const m = months();
    if (!id) {
      data.value = [];
      return;
    }
    loading.value = true;
    try {
      data.value = await loadOrCache(service, id, m);
    } catch (e) {
      console.error('Failed to load monthly aggregates', e);
      data.value = [];
    } finally {
      loading.value = false;
    }
  }

  watch([accountId, months], load, {immediate: true});

  return {data, loading, reload: load};
}

export function invalidateMonthlyAggregates() {
  cache.clear();
}
