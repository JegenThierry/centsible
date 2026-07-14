import type {NetWorthPoint} from "~/models/reports/net-worth-point";
import type {Currency} from "~/models/budget-account/currency";

/** A single projected recurring occurrence, already converted to the forecast currency. */
export interface ForecastOccurrence {
  date: string;
  description: string;
  /** Positive magnitude; direction is carried by {@link type}. */
  amount: number;
  type: 'INCOME' | 'EXPENSE';
  categoryName?: string | null;
  categoryColor?: string | null;
}

/** Forward-looking net-worth projection stepped from today by the active recurring rules. */
export interface NetWorthForecast {
  currency: Currency;
  points: NetWorthPoint[];
  occurrences: ForecastOccurrence[];
}
