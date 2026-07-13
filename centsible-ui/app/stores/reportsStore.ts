import {defineStore} from 'pinia'
import adze from 'adze'
import {useReportsService} from "~/services/reports/reports-service";
import type {NetWorthPoint} from "~/models/reports/net-worth-point";
import type {AccountBalanceAtDate} from "~/models/reports/account-balance-at-date";
import type {CategorySpendingSeries} from "~/models/reports/category-spending";
import type {CashFlowPoint} from "~/models/reports/cash-flow";
import type {YearOverYear} from "~/models/reports/year-over-year";
import type {BudgetVsActualPeriod} from "~/models/reports/budget-vs-actual";
import type {SafeToSpend} from "~/models/reports/safe-to-spend";
import {isoDateRangeForMonthsBack} from "~/utils/date";

export const useReportsStore = defineStore('reportsStore', () => {
  const reportsService = useReportsService(useApi());
  const netWorth = ref<NetWorthPoint[]>([]);
  const categorySpending = ref<CategorySpendingSeries[]>([]);
  const cashFlow = ref<CashFlowPoint[]>([]);
  const yearOverYear = ref<YearOverYear | null>(null);
  const budgetVsActual = ref<BudgetVsActualPeriod[]>([]);
  const safeToSpend = ref<SafeToSpend | null>(null);
  const inflight = ref(0);
  const pending = computed(() => inflight.value > 0);
  // Set when a report load fails so consumers can show an error state with retry instead of an
  // empty chart that reads as "no data yet". Cleared by the consumer before it refetches.
  const error = ref(false);
  const safeToSpendError = ref(false);

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
      adze.ns('reports').error(`Failed to fetch ${label}`, error);
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
      () => { netWorth.value = []; error.value = true; },
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
      () => { categorySpending.value = []; error.value = true; },
    );
  }

  async function fetchCashFlow(range: RangeArg = 6) {
    const {startDate, endDate} = resolveRange(range);
    await runFetch(
      "cash flow report",
      async () => { cashFlow.value = await reportsService.fetchCashFlow(startDate, endDate); },
      () => { cashFlow.value = []; error.value = true; },
    );
  }

  async function fetchYearOverYear() {
    await runFetch(
      "year-over-year report",
      async () => { yearOverYear.value = await reportsService.fetchYearOverYear(); },
      () => { yearOverYear.value = null; error.value = true; },
    );
  }

  async function fetchBudgetVsActual(periods: number = 6) {
    await runFetch(
      "budget-vs-actual report",
      async () => { budgetVsActual.value = await reportsService.fetchBudgetVsActual(periods); },
      () => { budgetVsActual.value = []; error.value = true; },
    );
  }

  async function fetchSafeToSpend() {
    await runFetch(
      "safe-to-spend",
      async () => { safeToSpend.value = await reportsService.fetchSafeToSpend(); },
      () => { safeToSpend.value = null; safeToSpendError.value = true; },
    );
  }

  return {
    netWorth,
    categorySpending,
    cashFlow,
    yearOverYear,
    budgetVsActual,
    safeToSpend,
    pending,
    error,
    safeToSpendError,
    fetchNetWorth,
    fetchNetWorthBreakdown,
    fetchCategorySpending,
    fetchCashFlow,
    fetchYearOverYear,
    fetchBudgetVsActual,
    fetchSafeToSpend,
  }
});
