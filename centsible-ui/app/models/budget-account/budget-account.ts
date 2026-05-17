import type {Currency} from "~/models/budget-account/currency";

export interface BudgetAccountForm {
  name: string;
  initialBalance: number;
  currency: Currency;
}

export type CreateBudgetAccountForm = BudgetAccountForm;
export type UpdateBudgetAccountForm = BudgetAccountForm;

export interface BudgetAccount {
  id: string;
  name: string;
  balance: number;
  initialBalance: number;
  currency: Currency;
}

export interface BudgetAccountSnapshot {
  createdAt: string;
  balance: number;
}
