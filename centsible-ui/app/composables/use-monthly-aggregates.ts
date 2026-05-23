import {ref, watch} from 'vue';
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {MonthlyAggregate} from "~/models/transactions/transaction";
import {createAsyncCache} from "~/utils/async-cache";

type TransactionService = ReturnType<typeof useTransactionService>;

const cache = createAsyncCache<MonthlyAggregate[]>(() => []);

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
      data.value = await cache.loadOrCache(key(id, m), () => service.aggregateByMonth(id, m));
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
  cache.invalidate();
}
