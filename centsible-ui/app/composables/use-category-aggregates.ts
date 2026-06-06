import {ref, watch} from 'vue';
import adze from 'adze'
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {CategoryAggregate} from "~/models/transactions/transaction";
import {createAsyncCache} from "~/utils/async-cache";

type TransactionService = ReturnType<typeof useTransactionService>;

const cache = createAsyncCache<CategoryAggregate[]>(() => []);

function key(accountId: string, fromIso: string | null, toIso: string | null): string {
  return `${accountId}|${fromIso ?? ''}|${toIso ?? ''}`;
}

function fetchAggregates(
  service: TransactionService,
  accountId: string,
  fromIso: string | null,
  toIso: string | null,
): Promise<CategoryAggregate[]> {
  return service.aggregateByCategory(accountId, {
    fromDate: fromIso ?? undefined,
    toDate: toIso ?? undefined,
  });
}

export async function prefetchCategoryAggregates(
  service: TransactionService,
  accountId: string,
  fromIso: string | null,
  toIso: string | null,
): Promise<void> {
  if (!accountId) return;
  try {
    await cache.loadOrCache(key(accountId, fromIso, toIso), () =>
      fetchAggregates(service, accountId, fromIso, toIso),
    );
  } catch (e) {
    adze.ns('categories').error('Failed to prefetch category aggregates', e);
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
      data.value = await cache.loadOrCache(key(id, from, to), () =>
        fetchAggregates(service, id, from, to),
      );
    } catch (e) {
      adze.ns('categories').error('Failed to load category aggregates', e);
      data.value = [];
    } finally {
      loading.value = false;
    }
  }

  watch([accountId, fromIso, toIso], load, {immediate: true});

  return {data, loading, reload: load};
}

export function invalidateCategoryAggregates() {
  cache.invalidate();
}
