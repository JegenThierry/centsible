import type {Category} from "~/models/category/category";

export interface Budget {
  id: string;
  category: Category;
  amountLimit: number;
  amountSpent: number;
  period: string;
  createdAt: string;
  updatedAt: string;
}

export interface BudgetRequest {
  categoryId: number;
  amountLimit: number;
}

export interface BudgetForm {
  category: Category | undefined;
  amountLimit: number;
}
