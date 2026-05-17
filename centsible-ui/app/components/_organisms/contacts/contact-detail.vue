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
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import ExportButton from "~/components/_molecules/exports/export-button.vue";
import {useActiveCurrency} from "~/composables/use-active-currency";

const props = defineProps<{
  contactId: string;
}>();

const contactsStore = useContactsStore();
const loansStore = useLoansStore();
const budgetAccountsStore = useBudgetAccountsStore();
const toasts = useToasts();
const {t} = useI18n();

const fileInput = ref<HTMLInputElement | null>(null);
const isCreateLoanOpen = ref(false);
const isRecordRepaymentOpen = ref(false);
const isDeleteLoanOpen = ref(false);
const isEditContactOpen = ref(false);
const isDeleteContactOpen = ref(false);
const selectedLoan = ref<Loan>();

const contact = computed<Contact | undefined>(() => contactsStore.findContactById(props.contactId));
const loans = computed<Loan[]>(() => loansStore.loansByContact[props.contactId] ?? []);
const currency = useActiveCurrency();

const stats = computed(() => {
  if (!contact.value) return [];
  return [
    {labelKey: 'contacts.detail.stats.totalLent', value: Number(contact.value.totalLent)},
    {labelKey: 'contacts.detail.stats.totalOwed', value: Number(contact.value.totalOwed)},
    {labelKey: 'contacts.detail.stats.totalRepaid', value: Number(contact.value.totalRepaid), valueClass: 'text-success'},
    {labelKey: 'contacts.detail.stats.outstanding', value: Number(contact.value.outstanding), valueClass: 'text-warning'},
  ];
});

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
    console.error('Failed to load contact detail', error);
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
      <UButton class="mb-4"
               color="neutral"
               icon="i-lucide-arrow-left"
               size="sm"
               variant="ghost"
               @click="navigateTo('/contacts')">
        {{ t('contacts.detail.back') }}
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
                {{ t('contacts.detail.loanCount', {count: contact.openLoanCount}, contact.openLoanCount) }}
              </p>
            </div>
          </div>
          <div class="flex gap-2">
            <ExportButton
              :default-title="t('contacts.detail.exportDefaultTitle', {name: contact.name})"
              :params-builder="() => ({ kind: 'LENDINGS_PER_CONTACT', contactId: contactId })"
              :label="t('contacts.detail.exportLabel')"
              type="LENDINGS_PER_CONTACT"
            />
            <UButton color="neutral"
                     icon="i-lucide-pencil"
                     variant="outline"
                     @click="isEditContactOpen = true">
              {{ t('contacts.detail.edit') }}
            </UButton>
            <UButton color="error"
                     icon="i-lucide-trash"
                     variant="outline"
                     @click="isDeleteContactOpen = true">
              {{ t('contacts.detail.delete') }}
            </UButton>
          </div>
        </div>
      </UCard>

      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <UCard v-for="stat in stats" :key="stat.labelKey">
          <p class="text-xs text-muted">{{ t(stat.labelKey) }}</p>
          <p :class="['text-lg font-bold', stat.valueClass]">
            <BalanceNumberFormat :balance="stat.value" :currency="currency"/>
          </p>
        </UCard>
      </div>

      <div class="flex items-center justify-between mb-3">
        <h3 class="font-semibold">{{ t('contacts.detail.loansHeading') }}</h3>
        <UButton icon="i-lucide-plus" size="sm" @click="isCreateLoanOpen = true">
          {{ t('contacts.detail.recordLending') }}
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
