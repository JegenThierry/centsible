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
import type {NetWorthForecast} from "~/models/reports/forecast";
import {isoDateRangeForMonthsBack} from "~/utils/date";

export const useReportsStore = defineStore('reportsStore', () => {
  const reportsService = useReportsService(useApi());
  const netWorth = ref<NetWorthPoint[]>([]);
  const categorySpending = ref<CategorySpendingSeries[]>([]);
  const cashFlow = ref<CashFlowPoint[]>([]);
  // Same-shape cash flow for the immediately-preceding period, powering the KPI strip's deltas.
  const cashFlowPrevious = ref<CashFlowPoint[]>([]);
  const yearOverYear = ref<YearOverYear | null>(null);
  const budgetVsActual = ref<BudgetVsActualPeriod[]>([]);
  const safeToSpend = ref<SafeToSpend | null>(null);
  // The forecast is forward-looking and independent of the report date range, so it carries its own
  // loading/error/horizon state instead of the page-level inflight/error — toggling the horizon must
  // not skeleton the whole page.
  const forecast = ref<NetWorthForecast | null>(null);
  const forecastMonths = ref(6);
  const forecastLoading = ref(false);
  const forecastError = ref(false);
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

  async function fetchCashFlowPrevious(range: RangeArg = 6) {
    const {startDate, endDate} = resolveRange(range);
    await runFetch(
      "previous cash flow report",
      async () => { cashFlowPrevious.value = await reportsService.fetchCashFlow(startDate, endDate); },
      // A failed comparison fetch just hides the deltas — it must not error the whole page.
      () => { cashFlowPrevious.value = []; },
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

  async function fetchForecast(months: number = forecastMonths.value) {
    forecastMonths.value = months;
    forecastLoading.value = true;
    forecastError.value = false;
    try {
      forecast.value = await reportsService.fetchForecast(months);
    } catch (e) {
      adze.ns('reports').error('Failed to fetch forecast', e);
      forecast.value = null;
      forecastError.value = true;
    } finally {
      forecastLoading.value = false;
    }
  }

  return {
    netWorth,
    categorySpending,
    cashFlow,
    cashFlowPrevious,
    yearOverYear,
    budgetVsActual,
    safeToSpend,
    forecast,
    forecastMonths,
    forecastLoading,
    forecastError,
    pending,
    error,
    safeToSpendError,
    fetchNetWorth,
    fetchNetWorthBreakdown,
    fetchCategorySpending,
    fetchCashFlow,
    fetchCashFlowPrevious,
    fetchYearOverYear,
    fetchBudgetVsActual,
    fetchSafeToSpend,
    fetchForecast,
  }
});
