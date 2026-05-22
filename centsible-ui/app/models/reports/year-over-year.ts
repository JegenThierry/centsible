export interface YearOverYearTotals {
  thisYearIncome: number;
  thisYearExpense: number;
  lastYearIncome: number;
  lastYearExpense: number;
}

export interface YearOverYearCategory {
  categoryId: number;
  categoryName: string;
  categoryColor?: string | null;
  thisYearAmount: number;
  lastYearAmount: number;
}

export interface YearOverYear {
  thisYear: number;
  lastYear: number;
  totals: YearOverYearTotals;
  perCategory: YearOverYearCategory[];
}
