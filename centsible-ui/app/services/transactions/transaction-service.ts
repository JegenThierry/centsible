import type {AxiosInstance} from "axios";
import type {
  CategoryAggregate,
  MonthlyAggregate,
  SetBalanceRequest,
  Transaction,
  TransactionRequest
} from "~/models/transactions/transaction";
import type {ImportPayloadRow, ImportResult} from "~/models/transactions/csv-import";
import type {TransactionFilters} from "~/models/transactions/transaction-filters";
import {validateRequest} from "~/composables/use-api";

export function useTransactionService(api: AxiosInstance) {
  async function fetchTransactions(
    accountId: string,
    page: number = 1,
    size: number = 25,
    filters: TransactionFilters = {},
  ): Promise<Transaction[]> {
    const params: Record<string, unknown> = {page, size};
    if (filters.search) params.search = filters.search;
    if (filters.categoryIds?.length) params.categoryIds = filters.categoryIds.join(',');
    if (filters.fromDate) params.fromDate = filters.fromDate;
    if (filters.toDate) params.toDate = filters.toDate;
    if (filters.sort) params.sort = filters.sort;
    const response = await api.get<Transaction[]>(`/transactions/${encodeURIComponent(accountId)}`, {params});
    return validateRequest<Transaction[]>(response);
  }

  async function createTransaction(accountId: string, transaction: Partial<TransactionRequest>): Promise<Transaction> {
    const response = await api.post<Transaction>(`/transactions/${encodeURIComponent(accountId)}`, transaction);
    return validateRequest<Transaction>(response);
  }

  async function updateTransaction(accountId: string, transactionId: string, transaction: Partial<TransactionRequest>): Promise<Transaction> {
    const response = await api.put<Transaction>(`/transactions/${encodeURIComponent(accountId)}/${encodeURIComponent(transactionId)}`, transaction);
    return validateRequest<Transaction>(response);
  }

  async function deleteTransaction(accountId: string, transactionId: string): Promise<void> {
    const response = await api.delete(`/transactions/${encodeURIComponent(accountId)}/${encodeURIComponent(transactionId)}`);
    if (response.status !== 200 && response.status !== 204) {
      throw new Error(response.statusText);
    }
  }

  async function aggregateByCategory(
    accountId: string,
    options?: { month?: string; fromDate?: string; toDate?: string },
  ): Promise<CategoryAggregate[]> {
    const params: Record<string, string> = {};
    if (options?.month) params.month = options.month;
    if (options?.fromDate) params.fromDate = options.fromDate;
    if (options?.toDate) params.toDate = options.toDate;
    const response = await api.get<CategoryAggregate[]>(
      `/transactions/${encodeURIComponent(accountId)}/aggregates/by-category`,
      {params: Object.keys(params).length ? params : undefined}
    );
    return validateRequest<CategoryAggregate[]>(response);
  }

  async function aggregateByMonth(accountId: string, months: number = 6): Promise<MonthlyAggregate[]> {
    const response = await api.get<MonthlyAggregate[]>(
      `/transactions/${encodeURIComponent(accountId)}/aggregates/by-month`,
      {params: {months}}
    );
    return validateRequest<MonthlyAggregate[]>(response);
  }

  async function bulkDelete(accountId: string, ids: string[]): Promise<number> {
    const response = await api.post<{affected: number}>(
      `/transactions/${encodeURIComponent(accountId)}/bulk-delete`,
      {ids}
    );
    return validateRequest<{affected: number}>(response).affected;
  }

  async function bulkCategorize(accountId: string, ids: string[], categoryId: number): Promise<number> {
    const response = await api.post<{affected: number}>(
      `/transactions/${encodeURIComponent(accountId)}/bulk-categorize`,
      {ids, categoryId}
    );
    return validateRequest<{affected: number}>(response).affected;
  }

  async function importBatch(accountId: string, rows: ImportPayloadRow[]): Promise<ImportResult> {
    const response = await api.post<ImportResult>(
      `/transactions/${encodeURIComponent(accountId)}/import`,
      {rows}
    );
    return validateRequest<ImportResult>(response);
  }

  async function setAccountBalance(accountId: string, payload: SetBalanceRequest): Promise<Transaction> {
    const response = await api.post<Transaction>(
      `/transactions/${encodeURIComponent(accountId)}/set-balance`,
      payload,
    );
    return validateRequest<Transaction>(response);
  }

  return {
    fetchTransactions,
    createTransaction,
    updateTransaction,
    deleteTransaction,
    bulkDelete,
    bulkCategorize,
    aggregateByCategory,
    aggregateByMonth,
    importBatch,
    setAccountBalance,
  }
}
