import {defineStore} from 'pinia'
import {useReportsService} from "~/services/reports/reports-service";
import type {NetWorthPoint} from "~/models/reports/net-worth-point";
import {format, subMonths} from 'date-fns';

export const useReportsStore = defineStore('reportsStore', () => {
  const reportsService = useReportsService(useApi());
  const netWorth = ref<NetWorthPoint[]>([]);
  const pending = ref(false);

  async function fetchNetWorth(monthsBack: number = 6) {
    const endDate = format(new Date(), 'yyyy-MM-dd');
    const startDate = format(subMonths(new Date(), monthsBack), 'yyyy-MM-dd');

    pending.value = true;
    try {
      netWorth.value = await reportsService.fetchNetWorth(startDate, endDate);
    } catch (error) {
      console.error("Failed to fetch net worth report", error);
      netWorth.value = [];
    } finally {
      pending.value = false;
    }
  }

  return {
    netWorth,
    pending,
    fetchNetWorth,
  }
});
