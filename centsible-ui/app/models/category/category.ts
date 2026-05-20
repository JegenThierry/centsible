export enum CategoryType {
  INCOME = 'INCOME',
  EXPENSE = 'EXPENSE'
}

export interface Category {
  id: number;
  name: string;
  icon: string;
  color: string;
  type: CategoryType;
  system: boolean;
  systemKey?: string | null;
}

export interface CategoryForm {
  name: string;
  icon: string;
  color: string;
  type: CategoryType;
}

// Stable identifiers used by system categories that other code resolves at
// runtime. Set in the DB by migrations (see migration 28).
export const CategorySystemKey = {
  BalanceAdjustment: 'BALANCE_ADJUSTMENT',
} as const;
