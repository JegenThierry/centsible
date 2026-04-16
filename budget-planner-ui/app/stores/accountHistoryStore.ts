import {defineStore} from 'pinia'
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import type {BudgetAccountSnapshot} from "~/models/budget-account/budget-account";
import {format, subDays} from 'date-fns';

export const useAccountHistoryStore = defineStore('accountHistoryStore', () => {
  const accountService = useBudgetAccountService(useApi());
  const snapshots = ref<BudgetAccountSnapshot[]>([]);
  const pending = ref(false);

  async function fetchSnapshots(accountId: string) {
    const endDate = format(new Date(), 'yyyy-MM-dd');
    const startDate = format(subDays(new Date(), 30), 'yyyy-MM-dd');

    pending.value = true;
    try {
      snapshots.value = await accountService.fetchSnapshots(accountId, startDate, endDate);
    } catch (error) {
      console.error("Failed to fetch snapshots", error);
    } finally {
      pending.value = false;
    }
  }

  return {
    snapshots,
    pending,
    fetchSnapshots
  }
});
