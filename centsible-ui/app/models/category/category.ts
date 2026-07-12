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

/** Stable [systemKey] values for system categories, used to resolve them at runtime. */
export const CategorySystemKey = {
  BalanceAdjustment: 'BALANCE_ADJUSTMENT',
} as const;
