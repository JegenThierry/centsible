import {ref, watch} from 'vue';
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {CategoryAggregate} from "~/models/transactions/transaction";

type TransactionService = ReturnType<typeof useTransactionService>;

interface CacheEntry {
  data: CategoryAggregate[];
  promise: Promise<CategoryAggregate[]> | null;
}

const cache = new Map<string, CacheEntry>();

function key(accountId: string, fromIso: string | null, toIso: string | null): string {
  return `${accountId}|${fromIso ?? ''}|${toIso ?? ''}`;
}

async function loadOrCache(
  service: TransactionService,
  accountId: string,
  fromIso: string | null,
  toIso: string | null,
): Promise<CategoryAggregate[]> {
  const k = key(accountId, fromIso, toIso);
  const entry = cache.get(k);
  if (entry) {
    if (entry.promise) return entry.promise;
    return entry.data;
  }
  const promise = service.aggregateByCategory(accountId, {
    fromDate: fromIso ?? undefined,
    toDate: toIso ?? undefined,
  });
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

export async function prefetchCategoryAggregates(
  service: TransactionService,
  accountId: string,
  fromIso: string | null,
  toIso: string | null,
): Promise<void> {
  if (!accountId) return;
  try {
    await loadOrCache(service, accountId, fromIso, toIso);
  } catch (e) {
    console.error('Failed to prefetch category aggregates', e);
  }
}

export function useCategoryAggregates(
  accountId: () => string,
  fromIso: () => string | null,
  toIso: () => string | null,
) {
  const service = useTransactionService(useApi());
  const data = ref<CategoryAggregate[]>([]);
  const loading = ref(false);

  async function load() {
    const id = accountId();
    const from = fromIso();
    const to = toIso();
    if (!id) {
      data.value = [];
      return;
    }
    loading.value = true;
    try {
      data.value = await loadOrCache(service, id, from, to);
    } catch (e) {
      console.error('Failed to load category aggregates', e);
      data.value = [];
    } finally {
      loading.value = false;
    }
  }

  watch([accountId, fromIso, toIso], load, {immediate: true});

  return {data, loading, reload: load};
}

export function invalidateCategoryAggregates() {
  cache.clear();
}
