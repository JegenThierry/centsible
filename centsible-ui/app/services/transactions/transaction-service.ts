import type {AxiosInstance} from "axios";
import type {
  CategoryAggregate,
  MonthlyAggregate,
  Transaction,
  TransactionRequest
} from "~/models/transactions/transaction";
import type {ImportPayloadRow, ImportResult} from "~/models/transactions/csv-import";
import {validateRequest} from "~/composables/use-api";

export function useTransactionService(api: AxiosInstance) {
  async function fetchTransactions(accountId: string, page: number = 1, size: number = 25): Promise<Transaction[]> {
    const response = await api.get<Transaction[]>(`/transactions/${encodeURIComponent(accountId)}`, {
      params: {
        page,
        size
      }
    });
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

  async function aggregateByCategory(accountId: string, month?: string): Promise<CategoryAggregate[]> {
    const response = await api.get<CategoryAggregate[]>(
      `/transactions/${encodeURIComponent(accountId)}/aggregates/by-category`,
      {params: month ? {month} : undefined}
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

  async function importBatch(accountId: string, rows: ImportPayloadRow[]): Promise<ImportResult> {
    const response = await api.post<ImportResult>(
      `/transactions/${encodeURIComponent(accountId)}/import`,
      {rows}
    );
    return validateRequest<ImportResult>(response);
  }

  return {
    fetchTransactions,
    createTransaction,
    updateTransaction,
    deleteTransaction,
    aggregateByCategory,
    aggregateByMonth,
    importBatch,
  }
}
