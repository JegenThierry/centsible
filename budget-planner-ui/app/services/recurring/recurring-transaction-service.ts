import type {AxiosInstance} from "axios";
import type {
  RecurringTransaction,
  RecurringTransactionRequest
} from "~/models/recurring/recurring-transaction";
import {validateRequest} from "~/composables/use-api";

export function useRecurringTransactionService(api: AxiosInstance) {
  async function fetchAll(accountId?: string): Promise<RecurringTransaction[]> {
    const response = await api.get<RecurringTransaction[]>('/recurring-transactions', {
      params: accountId ? {accountId} : undefined,
    });
    return validateRequest<RecurringTransaction[]>(response);
  }

  async function create(accountId: string, payload: RecurringTransactionRequest): Promise<RecurringTransaction> {
    const response = await api.post<RecurringTransaction>(
      `/recurring-transactions/${encodeURIComponent(accountId)}`,
      payload,
    );
    return validateRequest<RecurringTransaction>(response);
  }

  async function update(id: string, payload: RecurringTransactionRequest): Promise<RecurringTransaction> {
    const response = await api.put<RecurringTransaction>(
      `/recurring-transactions/${encodeURIComponent(id)}`,
      payload,
    );
    return validateRequest<RecurringTransaction>(response);
  }

  async function remove(id: string): Promise<void> {
    const response = await api.delete(`/recurring-transactions/${encodeURIComponent(id)}`);
    if (response.status !== 204 && response.status !== 200) {
      throw new Error(response.statusText);
    }
  }

  async function pause(id: string): Promise<RecurringTransaction> {
    const response = await api.post<RecurringTransaction>(`/recurring-transactions/${encodeURIComponent(id)}/pause`);
    return validateRequest<RecurringTransaction>(response);
  }

  async function resume(id: string): Promise<RecurringTransaction> {
    const response = await api.post<RecurringTransaction>(`/recurring-transactions/${encodeURIComponent(id)}/resume`);
    return validateRequest<RecurringTransaction>(response);
  }

  return {
    fetchAll,
    create,
    update,
    remove,
    pause,
    resume,
  };
}
