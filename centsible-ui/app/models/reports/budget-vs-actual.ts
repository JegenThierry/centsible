export type BudgetPeriodType = 'MONTHLY' | 'QUARTERLY' | 'ANNUAL';

export interface BudgetVsActualEntry {
  categoryId: number;
  categoryName: string;
  categoryColor?: string | null;
  limit: number;
  spent: number;
}

export interface BudgetVsActualPeriod {
  periodKey: string;
  periodType: BudgetPeriodType;
  entries: BudgetVsActualEntry[];
}
