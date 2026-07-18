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

export type CategoryForm = Pick<Category, 'name' | 'icon' | 'color' | 'type'>;

export const CategorySystemKey = {
  BalanceAdjustment: 'BALANCE_ADJUSTMENT',
} as const;
