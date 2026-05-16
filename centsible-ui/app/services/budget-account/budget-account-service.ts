import type {AxiosInstance} from "axios";
import type {
  BudgetAccount,
  BudgetAccountSnapshot,
  CreateBudgetAccountForm,
  UpdateBudgetAccountForm
} from "~/models/budget-account/budget-account";
import {validateRequest} from "~/composables/use-api";

export function useBudgetAccountService(api: AxiosInstance) {
  async function fetchAccounts(): Promise<BudgetAccount[]> {
    const response = await api.get<BudgetAccount[]>('/budget-accounts');
    return validateRequest<BudgetAccount[]>(response);
  }

  async function fetchAccount(id: string): Promise<BudgetAccount> {
    const response = await api.get<BudgetAccount>(`/budget-accounts/${encodeURIComponent(id)}`);
    return validateRequest<BudgetAccount>(response);
  }

  async function fetchSnapshots(id: string, startDate: string, endDate: string): Promise<BudgetAccountSnapshot[]> {
    const response = await api.get<BudgetAccountSnapshot[]>(`/budget-accounts/${encodeURIComponent(id)}/snapshots`, {
      params: {startDate, endDate}
    });
    return validateRequest<BudgetAccountSnapshot[]>(response);
  }

  async function createAccount(createAccountForm: CreateBudgetAccountForm): Promise<BudgetAccount> {
    const response = await api.post<BudgetAccount>('/budget-accounts', createAccountForm);
    return validateRequest<BudgetAccount>(response);
  }

  async function updateAccount(id: string, updateAccountForm: UpdateBudgetAccountForm): Promise<BudgetAccount> {
    const response = await api.put<BudgetAccount>(`/budget-accounts/${encodeURIComponent(id)}`, updateAccountForm);
    return validateRequest<BudgetAccount>(response);
  }

  async function deleteAccount(id: string): Promise<BudgetAccount> {
    const response = await api.delete<BudgetAccount>(`/budget-accounts/${encodeURIComponent(id)}`);
    return validateRequest<BudgetAccount>(response);
  }

  return {
    fetchAccounts,
    fetchAccount,
    fetchSnapshots,
    createAccount,
    updateAccount,
    deleteAccount,
  }
}
