import type {AxiosInstance} from "axios";
import type {Budget, BudgetRequest} from "~/models/budget/budget";
import {validateRequest} from "~/composables/use-api";

export function useBudgetService(api: AxiosInstance) {
  async function fetchAll(month?: string): Promise<Budget[]> {
    const response = await api.get<Budget[]>('/budgets', {
      params: month ? {month} : undefined,
    });
    return validateRequest<Budget[]>(response);
  }

  async function create(payload: BudgetRequest): Promise<Budget> {
    const response = await api.post<Budget>('/budgets', payload);
    return validateRequest<Budget>(response);
  }

  async function update(id: string, payload: BudgetRequest): Promise<Budget> {
    const response = await api.put<Budget>(`/budgets/${encodeURIComponent(id)}`, payload);
    return validateRequest<Budget>(response);
  }

  async function remove(id: string): Promise<void> {
    const response = await api.delete(`/budgets/${encodeURIComponent(id)}`);
    if (response.status !== 204 && response.status !== 200) {
      throw new Error(response.statusText);
    }
  }

  return {fetchAll, create, update, remove};
}
