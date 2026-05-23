import {defineStore} from 'pinia';
import adze from 'adze'
import type {Budget} from "~/models/budget/budget";
import {useBudgetService} from "~/services/budget/budget-service";

export const useBudgetsStore = defineStore('budgetsStore', () => {
  const service = useBudgetService(useApi());
  const items = ref<Budget[]>([]);
  const history = ref<{month: string; budgets: Budget[]}[]>([]);
  const loading = ref(false);

  async function fetchCurrentMonth() {
    loading.value = true;
    try {
      items.value = await service.fetchAll();
    } catch (error) {
      adze.ns('budgets').error('Failed to fetch budgets', error);
      items.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function fetchForMonth(month: string) {
    loading.value = true;
    try {
      items.value = await service.fetchAll(month);
    } catch (error) {
      adze.ns('budgets').error('Failed to fetch budgets for month', month, error);
      items.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function fetchHistory(months: string[]) {
    loading.value = true;
    try {
      history.value = await Promise.all(
        months.map(async (m) => ({month: m, budgets: await service.fetchAll(m)})),
      );
    } catch (error) {
      adze.ns('budgets').error('Failed to fetch budget history', error);
      history.value = [];
    } finally {
      loading.value = false;
    }
  }

  return {items, history, loading, fetchCurrentMonth, fetchForMonth, fetchHistory};
});
