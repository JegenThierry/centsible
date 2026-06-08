import type {CategoryType} from "~/models/category/category";
import type {Transaction, TransactionForm, TransactionSplitRequest} from "~/models/transactions/transaction";

export function transactionType(tx: Pick<Transaction, 'type' | 'category'>): CategoryType {
  return tx.type ?? tx.category.type;
}

export type SplitResolution =
  | {splits: undefined, error?: undefined}
  | {splits: TransactionSplitRequest[], error?: undefined}
  | {splits?: undefined, error: 'incomplete' | 'unbalanced'};

/**
 * Turns a form's split rows into the API payload, or reports why they are invalid.
 * Returns `{splits: undefined}` for a simple (non-split) transaction, the request array when the
 * rows are complete and sum to the total, or `{error}` describing the first problem found.
 */
export function resolveSplitPayload(form: TransactionForm): SplitResolution {
  const rows = form.splits;
  if (!Array.isArray(rows) || rows.length === 0) return {splits: undefined};
  if (rows.length < 2 || rows.some((r) => !r.category?.id)) return {error: 'incomplete'};
  const sum = rows.reduce((acc, r) => acc + (Number(r.amount) || 0), 0);
  if (Math.abs(sum - form.amount) >= 0.005) return {error: 'unbalanced'};
  return {
    splits: rows.map((r) => ({categoryId: r.category!.id, amount: Number(r.amount), note: r.note || null})),
  };
}
