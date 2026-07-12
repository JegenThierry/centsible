import type {Currency} from "~/models/budget-account/currency";
import type {AccountType} from "~/models/budget-account/account-type";

export interface BudgetAccountForm {
  name: string;
  initialBalance: number;
  currency: Currency;
  type: AccountType;
}

export type CreateBudgetAccountForm = BudgetAccountForm;

/** Only name and type are editable after creation — balance and currency are fixed. */
export type UpdateBudgetAccountForm = Pick<BudgetAccountForm, 'name' | 'type'>;

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
