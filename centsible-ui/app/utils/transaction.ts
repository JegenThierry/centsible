import type {CategoryType} from "~/models/category/category";
import type {Transaction} from "~/models/transactions/transaction";

export function transactionType(tx: Pick<Transaction, 'type' | 'category'>): CategoryType {
  return tx.type ?? tx.category.type;
}
