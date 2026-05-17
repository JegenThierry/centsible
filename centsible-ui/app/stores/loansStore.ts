import {defineStore} from "pinia";
import type {Loan, LoanForm, Repayment, RepaymentForm} from "~/models/loan/loan";
import {useLoanService} from "~/services/loan/loan-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useContactsStore} from "~/stores/contactsStore";

export const useLoansStore = defineStore('loansStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const apiErrors = useApiErrors();
  const loanService = useLoanService(api);
  const contactsStore = useContactsStore();

  const loansByContact = ref<Record<string, Loan[]>>({});
  const repaymentsByLoan = ref<Record<string, Repayment[]>>({});
  const totalOutstanding = ref<number>(0);
  const pending = ref(false);

  async function refreshLoansForContact(contactId: string) {
    pending.value = true;
    try {
      const loans = await loanService.fetchLoans(contactId);
      loansByContact.value = {...loansByContact.value, [contactId]: loans};
    } catch (error) {
      apiErrors.toastError(error, "Failed to fetch loans", "Loans could not be loaded");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function refreshRepayments(loanId: string) {
    try {
      const repayments = await loanService.fetchRepayments(loanId);
      repaymentsByLoan.value = {...repaymentsByLoan.value, [loanId]: repayments};
    } catch (error) {
      apiErrors.toastError(error, "Failed to fetch repayments", "Repayments could not be loaded");
      throw error;
    }
  }

  async function refreshOutstanding() {
    try {
      totalOutstanding.value = await loanService.fetchOutstanding();
    } catch (error) {
      console.error('Failed to fetch outstanding total', error);
    }
  }

  async function createLoan(form: LoanForm): Promise<Loan | undefined> {
    pending.value = true;
    try {
      const loan = await loanService.createLoan(form);
      toasts.success("Loan recorded", "Your loan has been recorded");
      await Promise.all([
        refreshOutstanding(),
        contactsStore.fetchContact(loan.contact.id),
        refreshLoansForContact(loan.contact.id),
      ]);
      return loan;
    } catch (error) {
      apiErrors.toastError(error, "Failed to record loan", "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function recordRepayment(loanId: string, contactId: string, form: RepaymentForm) {
    pending.value = true;
    try {
      await loanService.recordRepayment(loanId, form);
      toasts.success("Repayment recorded", "The repayment has been recorded");
      await Promise.all([
        refreshLoansForContact(contactId),
        refreshRepayments(loanId),
        refreshOutstanding(),
        contactsStore.fetchContact(contactId),
      ]);
    } catch (error) {
      apiErrors.toastError(error, "Failed to record repayment", "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function deleteLoan(id: string, contactId: string) {
    pending.value = true;
    try {
      await loanService.deleteLoan(id);
      toasts.success("Loan deleted", "The loan has been removed");
      await Promise.all([
        refreshLoansForContact(contactId),
        refreshOutstanding(),
        contactsStore.fetchContact(contactId),
      ]);
    } catch (error) {
      apiErrors.toastError(error, "Failed to delete loan", "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  return {
    loansByContact,
    repaymentsByLoan,
    totalOutstanding,
    pending,
    refreshLoansForContact,
    refreshRepayments,
    refreshOutstanding,
    createLoan,
    recordRepayment,
    deleteLoan,
  }
});
