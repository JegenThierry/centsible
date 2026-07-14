import type {AxiosInstance} from "axios";
import type {NetWorthPoint} from "~/models/reports/net-worth-point";
import type {AccountBalanceAtDate} from "~/models/reports/account-balance-at-date";
import type {CategorySpendingSeries} from "~/models/reports/category-spending";
import type {CashFlowPoint} from "~/models/reports/cash-flow";
import type {YearOverYear} from "~/models/reports/year-over-year";
import type {BudgetVsActualPeriod} from "~/models/reports/budget-vs-actual";
import type {SafeToSpend} from "~/models/reports/safe-to-spend";
import type {NetWorthForecast} from "~/models/reports/forecast";
import {validateRequest} from "~/composables/use-api";

export function useReportsService(api: AxiosInstance) {
  async function fetchNetWorth(startDate: string, endDate: string): Promise<NetWorthPoint[]> {
    const response = await api.get<NetWorthPoint[]>('/reports/net-worth', {
      params: {startDate, endDate}
    });
    return validateRequest<NetWorthPoint[]>(response);
  }

  async function fetchNetWorthBreakdown(date: string): Promise<AccountBalanceAtDate[]> {
    const response = await api.get<AccountBalanceAtDate[]>('/reports/net-worth/breakdown', {
      params: {date},
    });
    return validateRequest<AccountBalanceAtDate[]>(response);
  }

  async function fetchCategorySpending(startDate: string, endDate: string): Promise<CategorySpendingSeries[]> {
    const response = await api.get<CategorySpendingSeries[]>('/reports/category-spending', {
      params: {startDate, endDate},
    });
    return validateRequest<CategorySpendingSeries[]>(response);
  }

  async function fetchCashFlow(startDate: string, endDate: string): Promise<CashFlowPoint[]> {
    const response = await api.get<CashFlowPoint[]>('/reports/cash-flow', {
      params: {startDate, endDate},
    });
    return validateRequest<CashFlowPoint[]>(response);
  }

  async function fetchYearOverYear(): Promise<YearOverYear> {
    const response = await api.get<YearOverYear>('/reports/year-over-year');
    return validateRequest<YearOverYear>(response);
  }

  async function fetchBudgetVsActual(periods: number = 6): Promise<BudgetVsActualPeriod[]> {
    const response = await api.get<BudgetVsActualPeriod[]>('/reports/budget-vs-actual', {
      params: {periods},
    });
    return validateRequest<BudgetVsActualPeriod[]>(response);
  }

  async function fetchSafeToSpend(): Promise<SafeToSpend> {
    const response = await api.get<SafeToSpend>('/reports/safe-to-spend');
    return validateRequest<SafeToSpend>(response);
  }

  async function fetchForecast(months: number = 6): Promise<NetWorthForecast> {
    const response = await api.get<NetWorthForecast>('/reports/forecast', {
      params: {months},
    });
    return validateRequest<NetWorthForecast>(response);
  }

  return {
    fetchNetWorth,
    fetchNetWorthBreakdown,
    fetchCategorySpending,
    fetchCashFlow,
    fetchYearOverYear,
    fetchBudgetVsActual,
    fetchSafeToSpend,
    fetchForecast,
  }
}
