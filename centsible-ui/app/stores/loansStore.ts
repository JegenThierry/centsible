import {defineStore} from "pinia";
import adze from 'adze'
import type {Loan, LoanForm, LoanUpdateForm, Repayment, RepaymentForm} from "~/models/loan/loan";
import {useLoanService} from "~/services/loan/loan-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useContactsStore} from "~/stores/contactsStore";

export const useLoansStore = defineStore('loansStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const apiErrors = useApiErrors();
  const {t} = useNuxtApp().$i18n;
  const loanService = useLoanService(api);
  const contactsStore = useContactsStore();

  const loansByContact = ref<Record<string, Loan[]>>({});
  const repaymentsByLoan = ref<Record<string, Repayment[]>>({});
  const totalOutstanding = ref<number>(0);
  const outstandingExcludedCount = ref<number>(0);
  const allLoans = ref<Loan[]>([]);
  const pending = ref(false);
  /**
   * Sticky "loaded at least once" flags. Distinct from `pending` (in-flight) and from zero-valued
   * data — consumers use these to skip a redundant fetch when the dashboard already warmed them.
   */
  const allLoansLoaded = ref(false);
  const outstandingLoaded = ref(false);

  async function refreshAllLoans() {
    pending.value = true;
    try {
      allLoans.value = await loanService.fetchLoans();
      allLoansLoaded.value = true;
    } catch (error) {
      apiErrors.toastError(error, t('contacts.loans.toasts.fetchFailedTitle'), t('contacts.loans.toasts.fetchFailedBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function refreshLoansForContact(contactId: string) {
    pending.value = true;
    try {
      const loans = await loanService.fetchLoans(contactId);
      loansByContact.value = {...loansByContact.value, [contactId]: loans};
    } catch (error) {
      apiErrors.toastError(error, t('contacts.loans.toasts.fetchFailedTitle'), t('contacts.loans.toasts.fetchFailedBody'));
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
      apiErrors.toastError(error, t('contacts.loans.toasts.repaymentsFailedTitle'), t('contacts.loans.toasts.repaymentsFailedBody'));
      throw error;
    }
  }

  async function refreshOutstanding() {
    try {
      const result = await loanService.fetchOutstanding();
      totalOutstanding.value = result.outstanding;
      outstandingExcludedCount.value = result.excludedCount;
      outstandingLoaded.value = true;
    } catch (error) {
      adze.ns('loans').error('Failed to fetch outstanding total', error);
    }
  }

  async function createLoan(form: LoanForm): Promise<Loan | undefined> {
    pending.value = true;
    try {
      const loan = await loanService.createLoan(form);
      toasts.success(t('contacts.loans.toasts.recordedTitle'), t('contacts.loans.toasts.recordedBody'));
      await Promise.all([
        refreshOutstanding(),
        contactsStore.fetchContact(loan.contact.id),
        refreshLoansForContact(loan.contact.id),
      ]);
      return loan;
    } catch (error) {
      apiErrors.toastError(error, t('contacts.loans.toasts.recordFailedTitle'), t('contacts.loans.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function updateLoan(id: string, contactId: string, form: LoanUpdateForm): Promise<Loan | undefined> {
    pending.value = true;
    try {
      const loan = await loanService.updateLoan(id, form);
      toasts.success(t('contacts.loans.toasts.updatedTitle'), t('contacts.loans.toasts.updatedBody'));
      await Promise.all([
        refreshAllLoans(),
        refreshOutstanding(),
        refreshLoansForContact(contactId),
        contactsStore.fetchContact(contactId),
      ]);
      return loan;
    } catch (error) {
      apiErrors.toastError(error, t('contacts.loans.toasts.updateFailedTitle'), t('contacts.loans.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function recordRepayment(loanId: string, contactId: string, form: RepaymentForm) {
    pending.value = true;
    try {
      await loanService.recordRepayment(loanId, form);
      toasts.success(t('contacts.loans.toasts.repaymentRecordedTitle'), t('contacts.loans.toasts.repaymentRecordedBody'));
      await Promise.all([
        refreshLoansForContact(contactId),
        refreshRepayments(loanId),
        refreshOutstanding(),
        contactsStore.fetchContact(contactId),
      ]);
    } catch (error) {
      apiErrors.toastError(error, t('contacts.loans.toasts.repaymentFailedTitle'), t('contacts.loans.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function deleteRepayment(loanId: string, repaymentId: string, contactId: string) {
    try {
      await loanService.deleteRepayment(loanId, repaymentId);
      toasts.success(t('contacts.loans.toasts.repaymentDeletedTitle'), t('contacts.loans.toasts.repaymentDeletedBody'));
      await Promise.all([
        refreshRepayments(loanId),
        refreshLoansForContact(contactId),
        refreshAllLoans(),
        refreshOutstanding(),
        contactsStore.fetchContact(contactId),
      ]);
    } catch (error) {
      apiErrors.toastError(error, t('contacts.loans.toasts.repaymentDeleteFailedTitle'), t('contacts.loans.toasts.genericErrorBody'));
      throw error;
    }
  }

  async function deleteLoan(id: string, contactId: string) {
    pending.value = true;
    try {
      await loanService.deleteLoan(id);
      toasts.success(t('contacts.loans.toasts.deletedTitle'), t('contacts.loans.toasts.deletedBody'));
      await Promise.all([
        refreshLoansForContact(contactId),
        refreshOutstanding(),
        contactsStore.fetchContact(contactId),
      ]);
    } catch (error) {
      apiErrors.toastError(error, t('contacts.loans.toasts.deleteFailedTitle'), t('contacts.loans.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  return {
    loansByContact,
    repaymentsByLoan,
    totalOutstanding,
    outstandingExcludedCount,
    allLoans,
    pending,
    allLoansLoaded,
    outstandingLoaded,
    refreshAllLoans,
    refreshLoansForContact,
    refreshRepayments,
    refreshOutstanding,
    createLoan,
    updateLoan,
    recordRepayment,
    deleteRepayment,
    deleteLoan,
  }
});
