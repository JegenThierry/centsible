import {defineStore} from 'pinia';
import adze from 'adze'
import type {Budget, BudgetSuggestion} from "~/models/budget/budget";
import {useBudgetService} from "~/services/budget/budget-service";
import {createLatestRequestGate} from "~/utils/latest-request";

export const useBudgetsStore = defineStore('budgetsStore', () => {
  const service = useBudgetService(useApi());
  const items = ref<Budget[]>([]);
  const history = ref<{month: string; budgets: Budget[]}[]>([]);
  const suggestions = ref<BudgetSuggestion[]>([]);
  const loading = ref(false);
  const error = ref(false);
  const historyLoading = ref(false);
  const historyError = ref(false);

  const itemsGate = createLatestRequestGate();

  /** Shared loader for `items`; a superseded response leaves state to whichever call outran it. */
  async function loadItems(month?: string) {
    const isLatest = itemsGate.begin();
    loading.value = true;
    error.value = false;
    try {
      const data = await service.fetchAll(month);
      if (isLatest()) items.value = data;
    } catch (e) {
      adze.ns('budgets').error('Failed to fetch budgets', {month: month ?? 'current'}, e);
      if (isLatest()) {
        items.value = [];
        error.value = true;
      }
    } finally {
      if (isLatest()) loading.value = false;
    }
  }

  function fetchCurrentMonth() {
    return loadItems();
  }

  function fetchForMonth(month: string) {
    return loadItems(month);
  }

  async function fetchHistory(months: string[]) {
    historyLoading.value = true;
    historyError.value = false;
    try {
      history.value = await Promise.all(
        months.map(async (m) => ({month: m, budgets: await service.fetchAll(m)})),
      );
    } catch (e) {
      adze.ns('budgets').error('Failed to fetch budget history', e);
      history.value = [];
      historyError.value = true;
    } finally {
      historyLoading.value = false;
    }
  }

  /** Recent average monthly spend per expense category; failures degrade to no suggestions. */
  async function fetchSuggestions(): Promise<BudgetSuggestion[]> {
    try {
      suggestions.value = await service.fetchSuggestions();
    } catch (e) {
      adze.ns('budgets').error('Failed to fetch budget suggestions', e);
      suggestions.value = [];
    }
    return suggestions.value;
  }

  /** Creates a monthly budget for every unbudgeted expense category with positive suggested spend. */
  function bulkCreateSuggested(): Promise<Budget[]> {
    return service.bulkCreateSuggested();
  }

  return {
    items,
    history,
    suggestions,
    loading,
    error,
    historyLoading,
    historyError,
    fetchCurrentMonth,
    fetchForMonth,
    fetchHistory,
    fetchSuggestions,
    bulkCreateSuggested,
  };
});
