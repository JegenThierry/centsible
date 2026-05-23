import {defineStore} from 'pinia'
import adze from 'adze'
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import type {BudgetAccountSnapshot} from "~/models/budget-account/budget-account";
import {daysAgoIsoDate, todayIsoDate} from "~/utils/date";

export const useAccountHistoryStore = defineStore('accountHistoryStore', () => {
  const accountService = useBudgetAccountService(useApi());
  const snapshots = ref<BudgetAccountSnapshot[]>([]);
  const pending = ref(false);

  async function fetchSnapshots(accountId: string) {
    const endDate = todayIsoDate();
    const startDate = daysAgoIsoDate(30);

    pending.value = true;
    try {
      snapshots.value = await accountService.fetchSnapshots(accountId, startDate, endDate);
    } catch (error) {
      adze.ns('budget-accounts').error("Failed to fetch snapshots", error);
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
