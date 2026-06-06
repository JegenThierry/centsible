import {type Category, type CategoryType} from "~/models/category/category";
import type {Currency} from "~/models/budget-account/currency";

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
  originalAmount?: number | null,
  originalCurrency?: Currency | null,
  exchangeRate?: number | null,
  rateDate?: string | null,
  transferGroupId?: string | null,
}

export interface TransactionRequest {
  amount: number,
  description: string,
  categoryId: number,
  transactionDate: string,
  type?: CategoryType,
  currency?: Currency,
}

export interface TransferRequest {
  amount: number,
  destinationAccountId: string,
  description: string,
  transactionDate: string,
}

export interface TransferForm {
  amount: number,
  sourceAccountId: string | undefined,
  destinationAccountId: string | undefined,
  description: string,
  transactionDate: string | undefined,
}

export interface TransferDetails {
  transferGroupId: string,
  sourceAccountId: string,
  destinationAccountId: string,
  amount: number,
  description: string | null,
  transactionDate: string,
}

export interface TransactionForm {
  amount: number,
  description: string,
  category: Category | undefined,
  type: CategoryType,
  transactionDate: string | undefined,
  currency: Currency,
}

export interface ConversionPreview {
  convertedAmount: number,
  originalAmount: number,
  originalCurrency: Currency,
  accountCurrency: Currency,
  rate: number,
  rateDate: string,
  sameCurrency: boolean,
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
