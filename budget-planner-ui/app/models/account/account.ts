import type {Currency} from "~/models/account/currency";

export interface CreateAccountForm {
    name: string;
    initialBalance: number;
    currency: Currency;
}

export interface UpdateAccountForm {
    name: string;
    initialBalance: number;
    currency: Currency;
}

export interface Account {
    id: string;
    name: string;
    balance: number;
    currency: Currency;
}