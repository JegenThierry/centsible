import {defineStore} from 'pinia'
import {useReportsService} from "~/services/reports/reports-service";
import type {NetWorthPoint} from "~/models/reports/net-worth-point";
import type {AccountBalanceAtDate} from "~/models/reports/account-balance-at-date";
import type {CategorySpendingSeries} from "~/models/reports/category-spending";
import type {CashFlowPoint} from "~/models/reports/cash-flow";
import type {YearOverYear} from "~/models/reports/year-over-year";
import type {BudgetVsActualPeriod} from "~/models/reports/budget-vs-actual";
import {isoDateRangeForMonthsBack} from "~/utils/date";

export const useReportsStore = defineStore('reportsStore', () => {
  const reportsService = useReportsService(useApi());
  const netWorth = ref<NetWorthPoint[]>([]);
  const categorySpending = ref<CategorySpendingSeries[]>([]);
  const cashFlow = ref<CashFlowPoint[]>([]);
  const yearOverYear = ref<YearOverYear | null>(null);
  const budgetVsActual = ref<BudgetVsActualPeriod[]>([]);
  const inflight = ref(0);
  const pending = computed(() => inflight.value > 0);

  /**
   * Accepts a months-back number for back-compat with the original preset-only flow, or an
   * explicit {startDate, endDate} for the custom date-range picker added in the imports sprint.
   */
  type RangeArg = number | {startDate: string; endDate: string};
  function resolveRange(arg: RangeArg): {startDate: string; endDate: string} {
    return typeof arg === 'number' ? isoDateRangeForMonthsBack(arg) : arg;
  }

  async function runFetch<T>(label: string, loader: () => Promise<T>, fallback: () => void) {
    inflight.value++;
    try {
      return await loader();
    } catch (error) {
      console.error(`Failed to fetch ${label}`, error);
      fallback();
    } finally {
      inflight.value--;
    }
  }

  async function fetchNetWorth(range: RangeArg = 6) {
    const {startDate, endDate} = resolveRange(range);
    await runFetch(
      "net worth report",
      async () => { netWorth.value = await reportsService.fetchNetWorth(startDate, endDate); },
      () => { netWorth.value = []; },
    );
  }

  async function fetchNetWorthBreakdown(date: string): Promise<AccountBalanceAtDate[]> {
    return reportsService.fetchNetWorthBreakdown(date);
  }

  async function fetchCategorySpending(range: RangeArg = 6) {
    const {startDate, endDate} = resolveRange(range);
    await runFetch(
      "category spending report",
      async () => { categorySpending.value = await reportsService.fetchCategorySpending(startDate, endDate); },
      () => { categorySpending.value = []; },
    );
  }

  async function fetchCashFlow(range: RangeArg = 6) {
    const {startDate, endDate} = resolveRange(range);
    await runFetch(
      "cash flow report",
      async () => { cashFlow.value = await reportsService.fetchCashFlow(startDate, endDate); },
      () => { cashFlow.value = []; },
    );
  }

  async function fetchYearOverYear() {
    await runFetch(
      "year-over-year report",
      async () => { yearOverYear.value = await reportsService.fetchYearOverYear(); },
      () => { yearOverYear.value = null; },
    );
  }

  async function fetchBudgetVsActual(periods: number = 6) {
    await runFetch(
      "budget-vs-actual report",
      async () => { budgetVsActual.value = await reportsService.fetchBudgetVsActual(periods); },
      () => { budgetVsActual.value = []; },
    );
  }

  return {
    netWorth,
    categorySpending,
    cashFlow,
    yearOverYear,
    budgetVsActual,
    pending,
    fetchNetWorth,
    fetchNetWorthBreakdown,
    fetchCategorySpending,
    fetchCashFlow,
    fetchYearOverYear,
    fetchBudgetVsActual,
  }
});
