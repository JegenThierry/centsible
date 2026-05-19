import {type Category} from "~/models/category/category";

export interface Transaction {
  id: string,
  amount: number,
  category: Category,
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
}

export interface TransactionForm {
  amount: number,
  description: string,
  category: Category | undefined,
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

