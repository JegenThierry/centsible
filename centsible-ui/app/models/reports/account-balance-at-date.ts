import type {Currency} from "~/models/budget-account/currency";

export interface AccountBalanceAtDate {
  accountId: string;
  accountName: string;
  currency: Currency;
  balance: number;
  date: string;
}
