import {ref, watch} from 'vue';
import adze from 'adze'
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {DailyAggregate} from "~/models/transactions/transaction";
import {createAsyncCache} from "~/utils/async-cache";
import {registerLedgerAggregateCache} from "~/utils/ledger-aggregates";

type TransactionService = ReturnType<typeof useTransactionService>;

const cache = registerLedgerAggregateCache(createAsyncCache<DailyAggregate[]>(() => []));

function key(accountId: string, days: number): string {
  return `${accountId}|${days}`;
}

/** Warms the shared cache that [useDailyAggregates] reads, for [accountId] over [days]; swallows errors. */
export async function prefetchDailyAggregates(
  service: TransactionService,
  accountId: string,
  days: number,
): Promise<void> {
  if (!accountId) return;
  try {
    await cache.loadOrCache(key(accountId, days), () => service.aggregateByDay(accountId, days));
  } catch (e) {
    adze.ns('dashboard').error('Failed to prefetch daily aggregates', e);
  }
}

export function useDailyAggregates(accountId: () => string, days: () => number) {
  const service = useTransactionService(useApi());
  const data = ref<DailyAggregate[]>([]);
  const loading = ref(false);
  let token = 0;

  async function load() {
    const current = ++token;
    const id = accountId();
    const d = days();
    if (!id) {
      data.value = [];
      return;
    }
    loading.value = true;
    try {
      const result = await cache.loadOrCache(key(id, d), () => service.aggregateByDay(id, d));
      if (current === token) data.value = result;
    } catch (e) {
      adze.ns('dashboard').error('Failed to load daily aggregates', e);
      if (current === token) data.value = [];
    } finally {
      if (current === token) loading.value = false;
    }
  }

  watch([accountId, days], load, {immediate: true});

  return {data, loading, reload: load};
}
