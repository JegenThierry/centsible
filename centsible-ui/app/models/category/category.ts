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
}

export interface CategoryForm {
  name: string;
  icon: string;
  color: string;
  type: CategoryType;
}
