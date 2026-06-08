import type {Currency} from "~/models/budget-account/currency";

export interface SafeToSpend {
  yearMonth: string;
  currency: Currency;
  actualIncome: number;
  upcomingIncome: number;
  expectedIncome: number;
  alreadySpent: number;
  upcomingExpenses: number;
  safeToSpend: number;
  daysRemaining: number;
  dailyAllowance: number;
}
