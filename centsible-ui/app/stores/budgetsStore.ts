import {defineStore} from 'pinia';
import adze from 'adze'
import type {Budget} from "~/models/budget/budget";
import {useBudgetService} from "~/services/budget/budget-service";

export const useBudgetsStore = defineStore('budgetsStore', () => {
  const service = useBudgetService(useApi());
  const items = ref<Budget[]>([]);
  const history = ref<{month: string; budgets: Budget[]}[]>([]);
  const loading = ref(false);
  const error = ref(false);
  const historyError = ref(false);

  async function fetchCurrentMonth() {
    loading.value = true;
    error.value = false;
    try {
      items.value = await service.fetchAll();
    } catch (e) {
      adze.ns('budgets').error('Failed to fetch budgets', e);
      items.value = [];
      error.value = true;
    } finally {
      loading.value = false;
    }
  }

  async function fetchForMonth(month: string) {
    loading.value = true;
    error.value = false;
    try {
      items.value = await service.fetchAll(month);
    } catch (e) {
      adze.ns('budgets').error('Failed to fetch budgets for month', month, e);
      items.value = [];
      error.value = true;
    } finally {
      loading.value = false;
    }
  }

  async function fetchHistory(months: string[]) {
    loading.value = true;
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
      loading.value = false;
    }
  }

  return {items, history, loading, error, historyError, fetchCurrentMonth, fetchForMonth, fetchHistory};
});
