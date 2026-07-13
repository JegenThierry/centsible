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

/** A budget's spendable limit including any carried-over rollover. */
export function effectiveLimit(budget: Budget): number {
  return budget.amountLimit + (budget.rolloverAmount ?? 0);
}

/** UProgress color for a budget usage ratio (spent / effective limit): >1 error, >=0.85 warning, else success. */
export function budgetBarColor(ratio: number): 'error' | 'warning' | 'success' {
  if (ratio > 1) return 'error';
  if (ratio >= 0.85) return 'warning';
  return 'success';
}
