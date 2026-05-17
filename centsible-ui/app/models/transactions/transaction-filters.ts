export type TransactionSort = 'DATE_DESC' | 'DATE_ASC' | 'AMOUNT_DESC' | 'AMOUNT_ASC';

export interface TransactionFilters {
  search?: string;
  categoryIds?: number[];
  fromDate?: string;
  toDate?: string;
  sort?: TransactionSort;
}
