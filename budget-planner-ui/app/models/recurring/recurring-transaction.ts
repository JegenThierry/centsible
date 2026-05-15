import {type Category} from "~/models/category/category";

export enum Frequency {
  DAILY = 'DAILY',
  WEEKLY = 'WEEKLY',
  MONTHLY = 'MONTHLY',
  YEARLY = 'YEARLY',
}

export const FREQUENCY_LABELS: Record<Frequency, string> = {
  [Frequency.DAILY]: 'Daily',
  [Frequency.WEEKLY]: 'Weekly',
  [Frequency.MONTHLY]: 'Monthly',
  [Frequency.YEARLY]: 'Yearly',
};

export interface RecurringTransaction {
  id: string;
  accountId: string;
  category: Category;
  amount: number;
  description: string;
  frequency: Frequency;
  startDate: string;
  endDate?: string | null;
  nextRunAt: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RecurringTransactionRequest {
  amount: number;
  description: string;
  categoryId: number;
  frequency: Frequency;
  startDate: string;
  endDate?: string | null;
  active: boolean;
}

export interface RecurringTransactionForm {
  amount: number;
  description: string;
  category: Category | undefined;
  frequency: Frequency;
  startDate: string | undefined;
  endDate?: string | undefined;
  active: boolean;
}
