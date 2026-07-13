export type TransactionSort = 'DATE_DESC' | 'DATE_ASC' | 'AMOUNT_DESC' | 'AMOUNT_ASC';

export type TransactionTypeFilter = 'INCOME' | 'EXPENSE';

export interface TransactionFilters {
  search?: string;
  categoryIds?: number[];
  tagIds?: number[];
  fromDate?: string;
  toDate?: string;
  type?: TransactionTypeFilter;
  amountMin?: number;
  amountMax?: number;
  sort?: TransactionSort;
}

export interface CategoryDrillPayload {
  categoryId: number;
  categoryName: string;
  fromDate: string | null;
  toDate: string | null;
}
