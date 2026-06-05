import {ref, watch} from 'vue';
import adze from 'adze'
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import type {BudgetAccountSnapshot} from "~/models/budget-account/budget-account";
import {createAsyncCache} from "~/utils/async-cache";

type AccountService = ReturnType<typeof useBudgetAccountService>;

const cache = createAsyncCache<BudgetAccountSnapshot[]>(() => []);

function key(accountId: string, fromIso: string | null, toIso: string | null): string {
  return `${accountId}|${fromIso ?? ''}|${toIso ?? ''}`;
}

function fetchSnapshots(
  service: AccountService,
  accountId: string,
  fromIso: string | null,
  toIso: string | null,
): Promise<BudgetAccountSnapshot[]> {
  if (!fromIso || !toIso) return Promise.resolve([]);
  return service.fetchSnapshots(accountId, fromIso, toIso);
}

export async function prefetchAccountSnapshots(
  service: AccountService,
  accountId: string,
  fromIso: string | null,
  toIso: string | null,
): Promise<void> {
  if (!accountId) return;
  try {
    await cache.loadOrCache(key(accountId, fromIso, toIso), () =>
      fetchSnapshots(service, accountId, fromIso, toIso),
    );
  } catch (e) {
    adze.ns('budget-accounts').error('Failed to prefetch account snapshots', e);
  }
}

export function useAccountSnapshots(
  accountId: () => string,
  fromIso: () => string | null,
  toIso: () => string | null,
) {
  const service = useBudgetAccountService(useApi());
  const data = ref<BudgetAccountSnapshot[]>([]);
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
        fetchSnapshots(service, id, from, to),
      );
    } catch (e) {
      adze.ns('budget-accounts').error('Failed to load account snapshots', e);
      data.value = [];
    } finally {
      loading.value = false;
    }
  }

  watch([accountId, fromIso, toIso], load, {immediate: true});

  return {data, loading, reload: load};
}

export function invalidateAccountSnapshots() {
  cache.invalidate();
}
