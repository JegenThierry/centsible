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
import {createLatestRequestGate} from "~/utils/latest-request";

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

  /**
   * One generation per range *batch*, not per fetch. The range-scoped reports race each other and
   * differ wildly in cost (net worth over 24 months is far slower than over 3), so a per-fetch token
   * would still let `cashFlow` and `cashFlowPrevious` settle from different batches and silently
   * corrupt the KPI period-delta. They stand or fall together.
   */
  const rangeGate = createLatestRequestGate();

  /**
   * [isLatest] gates the assignment rather than the request: a batch that a newer range superseded
   * still resolves, it just drops its payload instead of painting a stale range.
   */
  async function runFetch<T>(
    label: string,
    loader: () => Promise<T>,
    apply: (value: T) => void,
    fallback: () => void,
    isLatest: () => boolean,
  ) {
    inflight.value++;
    try {
      const value = await loader();
      if (isLatest()) apply(value);
    } catch (error) {
      adze.ns('reports').error(`Failed to fetch ${label}`, error);
      if (isLatest()) fallback();
    } finally {
      inflight.value--;
    }
  }

  /**
   * Loads every range-scoped report as one batch — the only way in, so the four can't be dispatched
   * out of step with each other.
   */
  async function fetchRangeReports(range: RangeArg, previousRange: RangeArg) {
    const isLatest = rangeGate.begin();
    error.value = false;
    await Promise.all([
      fetchNetWorth(range, isLatest),
      fetchCategorySpending(range, isLatest),
      fetchCashFlow(range, isLatest),
      fetchCashFlowPrevious(previousRange, isLatest),
    ]);
  }

  async function fetchNetWorth(range: RangeArg, isLatest: () => boolean) {
    const {startDate, endDate} = resolveRange(range);
    await runFetch(
      "net worth report",
      () => reportsService.fetchNetWorth(startDate, endDate),
      (value) => { netWorth.value = value; },
      () => { netWorth.value = []; error.value = true; },
      isLatest,
    );
  }

  async function fetchNetWorthBreakdown(date: string): Promise<AccountBalanceAtDate[]> {
    return reportsService.fetchNetWorthBreakdown(date);
  }

  async function fetchCategorySpending(range: RangeArg, isLatest: () => boolean) {
    const {startDate, endDate} = resolveRange(range);
    await runFetch(
      "category spending report",
      () => reportsService.fetchCategorySpending(startDate, endDate),
      (value) => { categorySpending.value = value; },
      () => { categorySpending.value = []; error.value = true; },
      isLatest,
    );
  }

  async function fetchCashFlow(range: RangeArg, isLatest: () => boolean) {
    const {startDate, endDate} = resolveRange(range);
    await runFetch(
      "cash flow report",
      () => reportsService.fetchCashFlow(startDate, endDate),
      (value) => { cashFlow.value = value; },
      () => { cashFlow.value = []; error.value = true; },
      isLatest,
    );
  }

  async function fetchCashFlowPrevious(range: RangeArg, isLatest: () => boolean) {
    const {startDate, endDate} = resolveRange(range);
    await runFetch(
      "previous cash flow report",
      () => reportsService.fetchCashFlow(startDate, endDate),
      (value) => { cashFlowPrevious.value = value; },
      // A failed comparison fetch just hides the deltas — it must not error the whole page.
      () => { cashFlowPrevious.value = []; },
      isLatest,
    );
  }

  // The reports below don't take the selected range, so nothing supersedes them: they're their own
  // batch of one.
  const always = () => true;

  async function fetchYearOverYear() {
    await runFetch(
      "year-over-year report",
      () => reportsService.fetchYearOverYear(),
      (value) => { yearOverYear.value = value; },
      () => { yearOverYear.value = null; error.value = true; },
      always,
    );
  }

  async function fetchBudgetVsActual(periods: number = 6) {
    await runFetch(
      "budget-vs-actual report",
      () => reportsService.fetchBudgetVsActual(periods),
      (value) => { budgetVsActual.value = value; },
      () => { budgetVsActual.value = []; error.value = true; },
      always,
    );
  }

  async function fetchSafeToSpend() {
    await runFetch(
      "safe-to-spend",
      () => reportsService.fetchSafeToSpend(),
      (value) => { safeToSpend.value = value; },
      () => { safeToSpend.value = null; safeToSpendError.value = true; },
      always,
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
    // The four range-scoped fetches stay private: they're only correct as one batch, so
    // `fetchRangeReports` is the only way to reach them.
    fetchRangeReports,
    fetchNetWorthBreakdown,
    fetchYearOverYear,
    fetchBudgetVsActual,
    fetchSafeToSpend,
    fetchForecast,
  }
});
