import type {AxiosInstance} from "axios";
import type {Budget, BudgetRequest} from "~/models/budget/budget";
import {crudResource, validateRequest} from "~/composables/use-api";

export function useBudgetService(api: AxiosInstance) {
  const resource = crudResource<Budget, string, BudgetRequest>(api, '/budgets');

  async function fetchAll(month?: string): Promise<Budget[]> {
    const response = await api.get<Budget[]>('/budgets', {
      params: month ? {month} : undefined,
    });
    return validateRequest<Budget[]>(response);
  }

  return {
    fetchAll,
    create: (payload: BudgetRequest): Promise<Budget> => resource.create(payload),
    update: (id: string, payload: BudgetRequest): Promise<Budget> => resource.update(id, payload),
    remove: (id: string): Promise<void> => resource.remove(id),
  };
}
