import type {AxiosInstance} from "axios";
import type {
  BudgetAccount,
  BudgetAccountSnapshot,
  CreateBudgetAccountForm,
  UpdateBudgetAccountForm
} from "~/models/budget-account/budget-account";
import {crudResource, validateRequest} from "~/composables/use-api";

export function useBudgetAccountService(api: AxiosInstance) {
  const resource = crudResource<BudgetAccount, string, CreateBudgetAccountForm, UpdateBudgetAccountForm>(api, '/budget-accounts');

  async function fetchSnapshots(id: string, startDate: string, endDate: string): Promise<BudgetAccountSnapshot[]> {
    const response = await api.get<BudgetAccountSnapshot[]>(`/budget-accounts/${encodeURIComponent(id)}/snapshots`, {
      params: {startDate, endDate}
    });
    return validateRequest<BudgetAccountSnapshot[]>(response);
  }

  return {
    fetchAccounts: (): Promise<BudgetAccount[]> => resource.list(),
    fetchAccount: (id: string): Promise<BudgetAccount> => resource.get(id),
    fetchSnapshots,
    createAccount: (form: CreateBudgetAccountForm): Promise<BudgetAccount> => resource.create(form),
    updateAccount: (id: string, form: UpdateBudgetAccountForm): Promise<BudgetAccount> => resource.update(id, form),
    deleteAccount: (id: string): Promise<void> => resource.remove(id),
  }
}
