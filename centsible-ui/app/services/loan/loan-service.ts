import type {AxiosInstance} from "axios";
import {assertStatus, crudResource, validateRequest} from "~/composables/use-api";
import {invalidateLedgerAggregates} from "~/utils/ledger-aggregates";
import type {Loan, LoanForm, LoanUpdateForm, Repayment, RepaymentForm, SplitToLoansRequest} from "~/models/loan/loan";

export function useLoanService(api: AxiosInstance) {
  // Only the two side-effect-free endpoints use the shared resource; the ledger-writing paths below
  // stay bespoke so they can invalidate aggregates (see the doc comments on createLoan/deleteLoan).
  const resource = crudResource<Loan, string, LoanForm, LoanUpdateForm>(api, '/loans');

  async function fetchLoans(contactId?: string): Promise<Loan[]> {
    const response = await api.get<Loan[]>('/loans', {
      params: contactId ? {contactId} : undefined,
    });
    return validateRequest<Loan[]>(response);
  }

  const fetchLoan = (id: string): Promise<Loan> => resource.get(id);

  /**
   * A loan with `affectBalance` writes a source transaction server-side, so this is a ledger write
   * and has to drop the dashboard's aggregates like any other. (The `updateLoan` /
   * `splitTransactionIntoLoans` paths only touch loan metadata and tracking-only IOUs, so they
   * leave the ledger — and the aggregates — alone.)
   */
  async function createLoan(form: LoanForm): Promise<Loan> {
    const response = await api.post<Loan>('/loans', form);
    const loan = validateRequest<Loan>(response);
    if (form.affectBalance) invalidateLedgerAggregates();
    return loan;
  }

  /** Splits an existing expense into one tracking-only IOU per share (money owed back to you). */
  async function splitTransactionIntoLoans(transactionId: string, request: SplitToLoansRequest): Promise<Loan[]> {
    const response = await api.post<Loan[]>(
      `/loans/from-transaction/${encodeURIComponent(transactionId)}`,
      request,
    );
    return validateRequest<Loan[]>(response);
  }

  const updateLoan = (id: string, form: LoanUpdateForm): Promise<Loan> => resource.update(id, form);

  /** Unconditional: whether this loan had a balance-affecting source transaction isn't known here. */
  async function deleteLoan(id: string): Promise<void> {
    assertStatus(await api.delete(`/loans/${encodeURIComponent(id)}`));
    invalidateLedgerAggregates();
  }

  async function fetchRepayments(loanId: string): Promise<Repayment[]> {
    const response = await api.get<Repayment[]>(`/loans/${encodeURIComponent(loanId)}/repayments`);
    return validateRequest<Repayment[]>(response);
  }

  /** Like {@link createLoan}, an `affectBalance` repayment writes a transaction server-side. */
  async function recordRepayment(loanId: string, form: RepaymentForm): Promise<Repayment> {
    const response = await api.post<Repayment>(
      `/loans/${encodeURIComponent(loanId)}/repayments`,
      form,
    );
    const repayment = validateRequest<Repayment>(response);
    if (form.affectBalance) invalidateLedgerAggregates();
    return repayment;
  }

  /** Unconditional, for the same reason as {@link deleteLoan}. */
  async function deleteRepayment(loanId: string, repaymentId: string): Promise<void> {
    assertStatus(await api.delete(
      `/loans/${encodeURIComponent(loanId)}/repayments/${encodeURIComponent(repaymentId)}`,
    ));
    invalidateLedgerAggregates();
  }

  /** Total amount still owed across loans; [excludedCount] counts loans omitted from the sum. */
  async function fetchOutstanding(): Promise<{ outstanding: number; excludedCount: number }> {
    const response = await api.get<{ outstanding: number; excludedCount: number }>('/loans/outstanding');
    return validateRequest<{ outstanding: number; excludedCount: number }>(response);
  }

  return {
    fetchLoans,
    fetchLoan,
    createLoan,
    splitTransactionIntoLoans,
    updateLoan,
    deleteLoan,
    fetchRepayments,
    recordRepayment,
    deleteRepayment,
    fetchOutstanding,
  }
}
