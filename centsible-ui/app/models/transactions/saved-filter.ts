import type {TransactionFilters} from "~/models/transactions/transaction-filters";

/** A named, reusable snapshot of the transaction filter set. Mirrors the backend SavedFilterDTO. */
export interface SavedFilter {
  id: number;
  name: string;
  filters: TransactionFilters;
  createdAt: string;
  modifiedAt: string;
}

export interface SavedFilterForm {
  name: string;
  filters: TransactionFilters;
}
