import type {Currency} from "~/models/budget-account/currency";
import type {AccountType} from "~/models/budget-account/account-type";

export interface BudgetAccountForm {
  name: string;
  initialBalance: number;
  currency: Currency;
  type: AccountType;
}

export type CreateBudgetAccountForm = BudgetAccountForm;
export type UpdateBudgetAccountForm = BudgetAccountForm;

export interface BudgetAccount {
  id: string;
  name: string;
  balance: number;
  initialBalance: number;
  currency: Currency;
  type: AccountType;
}

export interface BudgetAccountSnapshot {
  createdAt: string;
  balance: number;
}
