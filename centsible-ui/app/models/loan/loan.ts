import type {Contact} from "~/models/contact/contact";
import type {Transaction} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";

export interface Loan {
  id: string;
  contact: Contact;
  transaction?: Transaction;
  accountId?: string;
  affectsBalance: boolean;
  lentAmount: number;
  owedAmount: number;
  totalRepaid: number;
  outstanding: number;
  currency: Currency;
  interestRate?: number;
  loanDate?: string;
  description?: string;
  dueDate?: string;
  notes?: string;
  createdAt?: string;
  modifiedAt?: string;
}

export interface LoanForm {
  contactId?: string;
  newContactFirstName?: string;
  newContactLastName?: string;
  accountId?: string;
  affectBalance: boolean;
  lentAmount: number;
  owedAmount: number;
  currency?: Currency;
  interestRate?: number;
  description: string;
  transactionDate: string;
  dueDate?: string;
  notes?: string;
}

/** Balance-neutral edits only (see backend LoanUpdateForm). */
export interface LoanUpdateForm {
  description: string;
  owedAmount: number;
  interestRate?: number;
  dueDate?: string;
  notes?: string;
}

export interface Repayment {
  id: string;
  loanId: string;
  transaction?: Transaction;
  affectsBalance: boolean;
  amount: number;
  currency?: Currency;
  repaidAt?: string;
  createdAt?: string;
}

export interface RepaymentForm {
  accountId?: string;
  affectBalance: boolean;
  amount: number;
  description?: string;
  repaidAt: string;
}

/**
 * Coerce an optional numeric form field to a number, or undefined when blank/invalid. Number inputs
 * emit '' when cleared, which would otherwise be sent to the backend and fail BigDecimal parsing.
 */
export function cleanOptionalNumber(value: unknown): number | undefined {
  if (value === undefined || value === null || String(value).trim() === '') return undefined;
  const n = Number(value);
  return Number.isNaN(n) ? undefined : n;
}

export type LoanStatus = 'settled' | 'partial' | 'open';

export function loanStatus(loan: Loan): LoanStatus {
  if (Number(loan.outstanding) <= 0) return 'settled';
  if (Number(loan.totalRepaid) > 0) return 'partial';
  return 'open';
}
