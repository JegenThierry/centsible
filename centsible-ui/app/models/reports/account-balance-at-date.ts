import type {AccountType} from "~/models/budget-account/account-type";
import type {Currency} from "~/models/budget-account/currency";

export interface AccountBalanceAtDate {
  accountId: string;
  accountName: string;
  currency: Currency;
  type: AccountType;
  balance: number;
  /** balance converted into targetCurrency (the user's default); null when no FX rate could be resolved. */
  convertedBalance: number | null;
  targetCurrency: Currency;
  date: string;
}
