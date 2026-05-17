import {defineStore} from 'pinia';
import type {Budget} from "~/models/budget/budget";
import {useBudgetService} from "~/services/budget/budget-service";

export const useBudgetsStore = defineStore('budgetsStore', () => {
  const service = useBudgetService(useApi());
  const items = ref<Budget[]>([]);
  const loading = ref(false);

  async function fetchCurrentMonth() {
    loading.value = true;
    try {
      items.value = await service.fetchAll();
    } catch (error) {
      console.error('Failed to fetch budgets', error);
      items.value = [];
    } finally {
      loading.value = false;
    }
  }

  return {items, loading, fetchCurrentMonth};
});
