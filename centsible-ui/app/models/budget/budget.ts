import type {Category} from "~/models/category/category";

export type BudgetPeriodType = 'MONTHLY' | 'QUARTERLY' | 'ANNUAL';

export const BUDGET_PERIOD_TYPES: BudgetPeriodType[] = ['MONTHLY', 'QUARTERLY', 'ANNUAL'];

export interface Budget {
  id: string;
  category: Category;
  amountLimit: number;
  amountSpent: number;
  period: string;
  periodType: BudgetPeriodType;
  rolloverEnabled: boolean;
  rolloverAmount: number;
  createdAt: string;
  updatedAt: string;
}

export interface BudgetRequest {
  categoryId: number;
  amountLimit: number;
  periodType: BudgetPeriodType;
  rolloverEnabled: boolean;
}

export interface BudgetForm {
  category: Category | undefined;
  amountLimit: number;
  periodType: BudgetPeriodType;
  rolloverEnabled: boolean;
}
