import {type Category, type CategoryType} from "~/models/category/category";

export interface Transaction {
  id: string,
  amount: number,
  category: Category,
  type: CategoryType,
  description: string,
  transactionDate: string,
  createdAt: string,
  updatedAt: string,
  attachmentCount: number,
}

export interface TransactionRequest {
  amount: number,
  description: string,
  categoryId: number,
  transactionDate: string,
  type?: CategoryType,
}

export interface TransactionForm {
  amount: number,
  description: string,
  category: Category | undefined,
  type: CategoryType,
  transactionDate: string | undefined,
}

export interface SetBalanceRequest {
  newBalance: number,
  categoryId: number,
  description: string,
  transactionDate: string,
}

export interface SetBalanceForm {
  newBalance: number,
  category: Category | undefined,
  description: string,
  transactionDate: string | undefined,
}

export interface CategoryAggregate {
  categoryId: number,
  categoryName: string,
  categoryColor?: string | null,
  categoryIcon?: string | null,
  total: number,
}

export interface MonthlyAggregate {
  yearMonth: string,
  income: number,
  expense: number,
}

export interface DailyAggregate {
  date: string,
  income: number,
  expense: number,
}
