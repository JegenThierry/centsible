import {ref, watch} from 'vue';
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {MonthlyAggregate} from "~/models/transactions/transaction";

interface CacheEntry {
  data: MonthlyAggregate[];
  promise: Promise<MonthlyAggregate[]> | null;
}

const cache = new Map<string, CacheEntry>();

function key(accountId: string, months: number): string {
  return `${accountId}|${months}`;
}

/**
 * Shared monthly-aggregates fetch. Multiple components on the same page asking for the same
 * (accountId, months) pair get a single network call and share the resulting array.
 */
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
    const k = key(id, m);
    const entry = cache.get(k);
    if (entry) {
      data.value = entry.data;
      if (entry.promise) {
        loading.value = true;
        try {
          data.value = await entry.promise;
        } finally {
          loading.value = false;
        }
      }
      return;
    }

    loading.value = true;
    const promise = service.aggregateByMonth(id, m);
    cache.set(k, {data: [], promise});
    try {
      const result = await promise;
      cache.set(k, {data: result, promise: null});
      data.value = result;
    } catch (e) {
      cache.delete(k);
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
