import {type Category, type CategoryType} from "~/models/category/category";
import type {Currency} from "~/models/budget-account/currency";
import type {Tag} from "~/models/tag/tag";

export interface TransactionSplit {
  id?: string,
  category: Category,
  amount: number,
  note?: string | null,
}

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
  tags?: Tag[],
  splits?: TransactionSplit[],
}

export interface TransactionSplitRequest {
  categoryId: number,
  amount: number,
  note?: string | null,
}

export interface TransactionRequest {
  amount: number,
  description: string,
  categoryId: number,
  transactionDate: string,
  type?: CategoryType,
  currency?: Currency,
  splits?: TransactionSplitRequest[],
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

export interface TransactionSplitRow {
  category: Category | undefined,
  amount: number,
  note?: string,
}

export interface TransactionForm {
  amount: number,
  description: string,
  category: Category | undefined,
  type: CategoryType,
  transactionDate: string | undefined,
  currency: Currency,
  tagIds?: number[],
  splits?: TransactionSplitRow[],
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
