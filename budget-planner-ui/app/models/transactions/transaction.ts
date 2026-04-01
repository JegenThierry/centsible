import {type Category} from "~/models/category/category";

export enum TransactionType {
  INCOME = 'INCOME',
  EXPENSE = 'EXPENSE',
}

export interface Transaction {
  id: string,
  amount: number,
  category: Category,
  type: TransactionType,
  description: string,
  transactionDate: string,
  createdAt: string,
  updatedAt: string,
}

export interface TransactionRequest {
  amount: number,
  type: TransactionType,
  description: string,
  categoryId: number,
  transactionDate: string,
}

export interface TransactionForm {
  amount: number,
  type: TransactionType,
  description: string,
  category: Category | undefined,
  transactionDate: string | undefined
}

export const transactionTypes = [
  {label: 'Expense', value: TransactionType.EXPENSE},
  {label: 'Income', value: TransactionType.INCOME},
];
