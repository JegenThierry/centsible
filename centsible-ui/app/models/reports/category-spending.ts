export interface MonthlyCategoryAmount {
  yearMonth: string;
  amount: number;
}

export interface CategorySpendingSeries {
  categoryId: number;
  categoryName: string;
  categoryColor?: string | null;
  totals: MonthlyCategoryAmount[];
}
