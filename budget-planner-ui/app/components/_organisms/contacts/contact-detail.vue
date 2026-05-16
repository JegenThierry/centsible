<script lang="ts" setup>
import type {Contact} from "~/models/contact/contact";
import type {Loan} from "~/models/loan/loan";
import {useContactsStore} from "~/stores/contactsStore";
import {useLoansStore} from "~/stores/loansStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useToasts} from "~/services/toasts/toast-service";
import ContactAvatar from "~/components/_atoms/contacts/contact-avatar.vue";
import LoanTable from "~/components/_organisms/loans/loan-table.vue";
import CreateLoanModal from "~/components/_organisms/loans/modals/create-loan-modal.vue";
import RecordRepaymentModal from "~/components/_organisms/loans/modals/record-repayment-modal.vue";
import DeleteLoanModal from "~/components/_organisms/loans/modals/delete-loan-modal.vue";
import EditContactModal from "~/components/_organisms/contacts/modals/edit-contact-modal.vue";
import DeleteContactModal from "~/components/_organisms/contacts/modals/delete-contact-modal.vue";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import ExportButton from "~/components/_molecules/exports/export-button.vue";
import {Currency} from "~/models/budget-account/currency";

const props = defineProps<{
  contactId: string;
}>();

const contactsStore = useContactsStore();
const loansStore = useLoansStore();
const budgetAccountsStore = useBudgetAccountsStore();
const toasts = useToasts();

const fileInput = ref<HTMLInputElement | null>(null);
const isCreateLoanOpen = ref(false);
const isRecordRepaymentOpen = ref(false);
const isDeleteLoanOpen = ref(false);
const isEditContactOpen = ref(false);
const isDeleteContactOpen = ref(false);
const selectedLoan = ref<Loan>();

const contact = computed<Contact | undefined>(() => contactsStore.findContactById(props.contactId));
const loans = computed<Loan[]>(() => loansStore.loansByContact[props.contactId] ?? []);
const currency = computed(() => budgetAccountsStore.activeAccount?.currency ?? Currency.EUR);

function onEditAvatar() {
  fileInput.value?.click();
}

async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  try {
    await contactsStore.updateContactPicture(props.contactId, file);
  } finally {
    if (fileInput.value) fileInput.value.value = '';
  }
}

function openRepayment(loan: Loan) {
  selectedLoan.value = loan;
  isRecordRepaymentOpen.value = true;
}

function openDeleteLoan(loan: Loan) {
  selectedLoan.value = loan;
  isDeleteLoanOpen.value = true;
}

async function reloadAll() {
  await Promise.all([
    contactsStore.fetchContact(props.contactId),
    loansStore.refreshLoansForContact(props.contactId),
  ]);
}

function onContactDeleted() {
  toasts.success('Contact removed', 'Returning to contacts list.');
  navigateTo('/contacts');
}

onMounted(async () => {
  try {
    await Promise.all([
      contactsStore.fetchContact(props.contactId),
      loansStore.refreshLoansForContact(props.contactId),
      budgetAccountsStore.availableAccounts.length === 0
        ? budgetAccountsStore.updateAvailableAccounts()
        : Promise.resolve(),
    ]);
  } catch (error) {
    console.error('Failed to load contact detail', error);
    toasts.error('Failed to load contact', 'Please refresh the page to try again.');
  }
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <div v-if="!contact" class="flex justify-center py-20">
      <LoadingAnimation/>
    </div>

    <template v-else>
      <UButton class="mb-4"
               color="neutral"
               icon="i-lucide-arrow-left"
               size="sm"
               variant="ghost"
               @click="navigateTo('/contacts')">
        Back to contacts
      </UButton>

      <UCard class="mb-6">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <ContactAvatar :alt="contact.name"
                           :src="contact.picture"
                           editable
                           size="3xl"
                           @edit="onEditAvatar"/>
            <input ref="fileInput"
                   accept="image/*"
                   class="hidden"
                   type="file"
                   @change="onFileChange"/>
            <div>
              <h2 class="text-2xl font-bold">{{ contact.name }}</h2>
              <p class="text-sm text-neutral-500">
                {{ contact.openLoanCount }} {{ contact.openLoanCount === 1 ? 'loan' : 'loans' }}
              </p>
            </div>
          </div>
          <div class="flex gap-2">
            <ExportButton
              :default-title="`Lendings — ${contact.name}`"
              :params-builder="() => ({ kind: 'LENDINGS_PER_CONTACT', contactId: contactId })"
              label="Export"
              type="LENDINGS_PER_CONTACT"
            />
            <UButton color="neutral"
                     icon="i-lucide-pencil"
                     variant="outline"
                     @click="isEditContactOpen = true">
              Edit
            </UButton>
            <UButton color="error"
                     icon="i-lucide-trash"
                     variant="outline"
                     @click="isDeleteContactOpen = true">
              Delete
            </UButton>
          </div>
        </div>
      </UCard>

      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <UCard>
          <p class="text-xs text-muted">Total lent</p>
          <p class="text-lg font-bold">
            <BalanceNumberFormat :balance="Number(contact.totalLent)" :currency="currency"/>
          </p>
        </UCard>
        <UCard>
          <p class="text-xs text-muted">Total owed</p>
          <p class="text-lg font-bold">
            <BalanceNumberFormat :balance="Number(contact.totalOwed)" :currency="currency"/>
          </p>
        </UCard>
        <UCard>
          <p class="text-xs text-muted">Total repaid</p>
          <p class="text-lg font-bold text-success">
            <BalanceNumberFormat :balance="Number(contact.totalRepaid)" :currency="currency"/>
          </p>
        </UCard>
        <UCard>
          <p class="text-xs text-muted">Outstanding</p>
          <p class="text-lg font-bold text-warning">
            <BalanceNumberFormat :balance="Number(contact.outstanding)" :currency="currency"/>
          </p>
        </UCard>
      </div>

      <div class="flex items-center justify-between mb-3">
        <h3 class="font-semibold">Loans</h3>
        <UButton icon="i-lucide-plus" size="sm" @click="isCreateLoanOpen = true">
          Record Lending
        </UButton>
      </div>

      <LoanTable :loans="loans"
                 :loading="loansStore.pending"
                 @repay="openRepayment"
                 @delete="openDeleteLoan"/>

      <EditContactModal v-model:open="isEditContactOpen" :contact="contact"/>
      <DeleteContactModal v-model:open="isDeleteContactOpen" :contact="contact" @deleted="onContactDeleted"/>
      <CreateLoanModal v-if="isCreateLoanOpen"
                       v-model:open="isCreateLoanOpen"
                       :contact-id="contactId"
                       @created="reloadAll"/>
      <RecordRepaymentModal v-if="isRecordRepaymentOpen"
                            v-model:open="isRecordRepaymentOpen"
                            :loan="selectedLoan"
                            @recorded="reloadAll"/>
      <DeleteLoanModal v-if="isDeleteLoanOpen"
                       v-model:open="isDeleteLoanOpen"
                       :loan="selectedLoan"
                       @deleted="reloadAll"/>
    </template>
  </UContainer>
</template>
