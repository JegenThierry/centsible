import type {Currency} from "~/models/budget-account/currency";

export interface CreateBudgetAccountForm {
    name: string;
    initialBalance: number;
    currency: Currency;
}

export interface UpdateBudgetAccountForm {
    name: string;
    initialBalance: number;
    currency: Currency;
}

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
