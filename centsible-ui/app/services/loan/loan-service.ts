import type {AxiosInstance} from "axios";
import {assertStatus, validateRequest} from "~/composables/use-api";
import type {Loan, LoanForm, LoanUpdateForm, Repayment, RepaymentForm} from "~/models/loan/loan";

export function useLoanService(api: AxiosInstance) {
  async function fetchLoans(contactId?: string): Promise<Loan[]> {
    const response = await api.get<Loan[]>('/loans', {
      params: contactId ? {contactId} : undefined,
    });
    return validateRequest<Loan[]>(response);
  }

  async function fetchLoan(id: string): Promise<Loan> {
    const response = await api.get<Loan>(`/loans/${encodeURIComponent(id)}`);
    return validateRequest<Loan>(response);
  }

  async function createLoan(form: LoanForm): Promise<Loan> {
    const response = await api.post<Loan>('/loans', form);
    return validateRequest<Loan>(response);
  }

  async function updateLoan(id: string, form: LoanUpdateForm): Promise<Loan> {
    const response = await api.put<Loan>(`/loans/${encodeURIComponent(id)}`, form);
    return validateRequest<Loan>(response);
  }

  async function deleteLoan(id: string): Promise<void> {
    assertStatus(await api.delete(`/loans/${encodeURIComponent(id)}`));
  }

  async function fetchRepayments(loanId: string): Promise<Repayment[]> {
    const response = await api.get<Repayment[]>(`/loans/${encodeURIComponent(loanId)}/repayments`);
    return validateRequest<Repayment[]>(response);
  }

  async function recordRepayment(loanId: string, form: RepaymentForm): Promise<Repayment> {
    const response = await api.post<Repayment>(
      `/loans/${encodeURIComponent(loanId)}/repayments`,
      form,
    );
    return validateRequest<Repayment>(response);
  }

  async function deleteRepayment(loanId: string, repaymentId: string): Promise<void> {
    assertStatus(await api.delete(
      `/loans/${encodeURIComponent(loanId)}/repayments/${encodeURIComponent(repaymentId)}`,
    ));
  }

  async function fetchOutstanding(): Promise<{ outstanding: number; excludedCount: number }> {
    const response = await api.get<{ outstanding: number; excludedCount: number }>('/loans/outstanding');
    return validateRequest<{ outstanding: number; excludedCount: number }>(response);
  }

  return {
    fetchLoans,
    fetchLoan,
    createLoan,
    updateLoan,
    deleteLoan,
    fetchRepayments,
    recordRepayment,
    deleteRepayment,
    fetchOutstanding,
  }
}
