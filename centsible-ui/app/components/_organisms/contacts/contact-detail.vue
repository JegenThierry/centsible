<script lang="ts" setup>
import adze from 'adze'
import type {Contact} from "~/models/contact/contact";
import type {Loan} from "~/models/loan/loan";
import {useContactsStore} from "~/stores/contactsStore";
import {useLoansStore} from "~/stores/loansStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useToasts} from "~/services/toasts/toast-service";
import ContactDetailHeader from "~/components/_organisms/contacts/contact-detail-header.vue";
import ContactStatsGrid from "~/components/_molecules/contacts/contact-stats-grid.vue";
import LoanTable from "~/components/_organisms/loans/loan-table.vue";
const CreateLoanModal = defineAsyncComponent(() => import("~/components/_organisms/loans/modals/create-loan-modal.vue"));
const EditLoanModal = defineAsyncComponent(() => import("~/components/_organisms/loans/modals/edit-loan-modal.vue"));
const RecordRepaymentModal = defineAsyncComponent(() => import("~/components/_organisms/loans/modals/record-repayment-modal.vue"));
const RepaymentsModal = defineAsyncComponent(() => import("~/components/_organisms/loans/modals/repayments-modal.vue"));
const DeleteLoanModal = defineAsyncComponent(() => import("~/components/_organisms/loans/modals/delete-loan-modal.vue"));
const EditContactModal = defineAsyncComponent(() => import("~/components/_organisms/contacts/modals/edit-contact-modal.vue"));
const DeleteContactModal = defineAsyncComponent(() => import("~/components/_organisms/contacts/modals/delete-contact-modal.vue"));
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useActiveCurrency} from "~/composables/use-active-currency";

const props = defineProps<{
  contactId: string;
}>();

const contactsStore = useContactsStore();
const loansStore = useLoansStore();
const budgetAccountsStore = useBudgetAccountsStore();
const toasts = useToasts();
const {t} = useI18n();

const isCreateLoanOpen = ref(false);
const isEditLoanOpen = ref(false);
const isRecordRepaymentOpen = ref(false);
const isRepaymentsOpen = ref(false);
const isDeleteLoanOpen = ref(false);
const isEditContactOpen = ref(false);
const isDeleteContactOpen = ref(false);
const selectedLoan = ref<Loan>();

const contact = computed<Contact | undefined>(() => contactsStore.findContactById(props.contactId));
const loans = computed<Loan[]>(() => loansStore.loansByContact[props.contactId] ?? []);
const currency = useActiveCurrency();

function openRepayment(loan: Loan) {
  selectedLoan.value = loan;
  isRecordRepaymentOpen.value = true;
}

function openRepayments(loan: Loan) {
  selectedLoan.value = loan;
  isRepaymentsOpen.value = true;
}

function openEditLoan(loan: Loan) {
  selectedLoan.value = loan;
  isEditLoanOpen.value = true;
}

function openDeleteLoan(loan: Loan) {
  selectedLoan.value = loan;
  isDeleteLoanOpen.value = true;
}

function reloadAll() {
  return Promise.all([
    contactsStore.fetchContact(props.contactId),
    loansStore.refreshLoansForContact(props.contactId),
  ]);
}

function onContactDeleted() {
  toasts.success(t('contacts.toasts.removedTitle'), t('contacts.toasts.removedBody'));
  navigateTo('/contacts');
}

onMounted(async () => {
  try {
    await Promise.all([
      reloadAll(),
      budgetAccountsStore.availableAccounts.length === 0
        ? budgetAccountsStore.updateAvailableAccounts()
        : Promise.resolve(),
    ]);
  } catch (error) {
    adze.ns('contacts').error('Failed to load contact detail', error);
    toasts.error(t('contacts.toasts.loadDetailFailedTitle'), t('contacts.toasts.loadFailedBody'));
  }
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <div v-if="!contact" class="flex justify-center py-20">
      <LoadingAnimation/>
    </div>

    <template v-else>
      <AppButton class="mb-4"
                 color="neutral"
                 icon="i-lucide-arrow-left"
                 size="sm"
                 variant="ghost"
                 @click="navigateTo('/contacts')">
        {{ t('contacts.detail.back') }}
      </AppButton>

      <ContactDetailHeader :contact="contact"
                           @edit="isEditContactOpen = true"
                           @delete="isDeleteContactOpen = true"/>

      <ContactStatsGrid :contact="contact" :currency="currency"/>

      <div class="flex items-center justify-between mb-3">
        <h3 class="font-semibold">{{ t('contacts.detail.loansHeading') }}</h3>
        <AppButton icon="i-lucide-plus" size="sm" @click="isCreateLoanOpen = true">
          {{ t('contacts.detail.recordLending') }}
        </AppButton>
      </div>

      <LoanTable :loans="loans"
                 :loading="loansStore.pending"
                 @repay="openRepayment"
                 @repayments="openRepayments"
                 @edit="openEditLoan"
                 @delete="openDeleteLoan"/>

      <EditContactModal v-model:open="isEditContactOpen" :contact="contact"/>
      <DeleteContactModal v-model:open="isDeleteContactOpen" :contact="contact" @deleted="onContactDeleted"/>
      <CreateLoanModal v-if="isCreateLoanOpen"
                       v-model:open="isCreateLoanOpen"
                       :contact-id="contactId"
                       @created="reloadAll"/>
      <EditLoanModal v-if="isEditLoanOpen"
                     v-model:open="isEditLoanOpen"
                     :loan="selectedLoan"
                     @updated="reloadAll"/>
      <RecordRepaymentModal v-if="isRecordRepaymentOpen"
                            v-model:open="isRecordRepaymentOpen"
                            :loan="selectedLoan"
                            @recorded="reloadAll"/>
      <RepaymentsModal v-if="isRepaymentsOpen"
                       v-model:open="isRepaymentsOpen"
                       :loan="selectedLoan"/>
      <DeleteLoanModal v-if="isDeleteLoanOpen"
                       v-model:open="isDeleteLoanOpen"
                       :loan="selectedLoan"
                       @deleted="reloadAll"/>
    </template>
  </UContainer>
</template>
