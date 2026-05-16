import type {Contact} from "~/models/contact/contact";
import type {Transaction} from "~/models/transactions/transaction";

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
  description: string;
  transactionDate: string;
  dueDate?: string;
  notes?: string;
}

export interface Repayment {
  id: string;
  loanId: string;
  transaction?: Transaction;
  affectsBalance: boolean;
  amount: number;
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
