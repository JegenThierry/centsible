<script lang="ts" setup>
import {useContactsStore} from "~/stores/contactsStore";
import {useLoansStore} from "~/stores/loansStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import CreateContactModal from "~/components/_organisms/contacts/modals/create-contact-modal.vue";
import EditContactModal from "~/components/_organisms/contacts/modals/edit-contact-modal.vue";
import DeleteContactModal from "~/components/_organisms/contacts/modals/delete-contact-modal.vue";
import CreateLoanModal from "~/components/_organisms/loans/modals/create-loan-modal.vue";
import ContactCard from "~/components/_organisms/cards/contact-card.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";
import ExportButton from "~/components/_molecules/exports/export-button.vue";
import {todayIsoDate} from "~/utils/date";
import {Currency} from "~/models/budget-account/currency";
import type {Contact} from "~/models/contact/contact";

const contactsStore = useContactsStore();
const loansStore = useLoansStore();
const budgetAccountsStore = useBudgetAccountsStore();

const isCreateContactOpen = ref(false);
const isEditContactOpen = ref(false);
const isDeleteContactOpen = ref(false);
const isCreateLoanOpen = ref(false);
const selectedContact = ref<Contact>();

const currency = computed(() => budgetAccountsStore.activeAccount?.currency ?? Currency.EUR);

function openDetail(contact: Contact) {
  navigateTo(`/contacts/${contact.id}`);
}

function openEdit(contact: Contact) {
  selectedContact.value = contact;
  isEditContactOpen.value = true;
}

function openDelete(contact: Contact) {
  selectedContact.value = contact;
  isDeleteContactOpen.value = true;
}

onMounted(() => {
  Promise.all([
    contactsStore.updateContacts(),
    loansStore.refreshOutstanding(),
    budgetAccountsStore.availableAccounts.length === 0
      ? budgetAccountsStore.updateAvailableAccounts()
      : Promise.resolve(),
  ]);
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader description="Track people who owe you money."
                title="Contacts">
      <template #actions>
        <ExportButton
          v-if="contactsStore.contacts.length > 0"
          :default-title="`All Lendings ${todayIsoDate()}`"
          :params-builder="() => ({ kind: 'LENDINGS_ALL', includeSettled: true })"
          label="Export lendings"
          type="LENDINGS_ALL"
        />
        <UButton class="w-full sm:w-auto justify-center"
                 icon="i-lucide-hand-coins"
                 variant="outline"
                 @click="isCreateLoanOpen = true">
          Record Lending
        </UButton>
        <UButton class="w-full sm:w-auto justify-center"
                 icon="i-lucide-plus"
                 @click="isCreateContactOpen = true">
          Add Contact
        </UButton>
      </template>
    </PageHeader>

    <UCard v-if="loansStore.totalOutstanding > 0" class="mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <UIcon class="w-8 h-8 text-amber-500" name="i-lucide-hand-coins"/>
          <div>
            <p class="text-sm text-neutral-500">People owe you</p>
            <p class="text-2xl font-bold text-amber-600 dark:text-amber-400">
              <BalanceNumberFormat :balance="Number(loansStore.totalOutstanding)" :currency="currency" format="de-De"/>
            </p>
          </div>
        </div>
      </div>
    </UCard>

    <div v-if="contactsStore.pending && contactsStore.contacts.length > 0" class="flex justify-center mb-6">
      <LoadingAnimation/>
    </div>

    <AppEmptyState v-if="contactsStore.contacts.length === 0 && !contactsStore.pending"
                   description="Add a contact or record your first loan to start tracking."
                   icon="i-lucide-users"
                   title="No contacts yet">
      <template #actions>
        <UButton class="w-full sm:w-auto justify-center" @click="isCreateContactOpen = true">Add Contact</UButton>
      </template>
    </AppEmptyState>

    <template v-else-if="contactsStore.pending && contactsStore.contacts.length === 0">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
        <CardSkeleton v-for="i in 4" :key="i"/>
      </div>
    </template>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
      <ContactCard v-for="c in contactsStore.contacts"
                   :key="c.id"
                   :contact="c"
                   @open="openDetail"
                   @edit="openEdit"
                   @delete="openDelete"/>
    </div>

    <CreateContactModal v-model:open="isCreateContactOpen"/>
    <EditContactModal v-model:open="isEditContactOpen" :contact="selectedContact"/>
    <DeleteContactModal v-model:open="isDeleteContactOpen" :contact="selectedContact"/>
    <CreateLoanModal v-if="isCreateLoanOpen" v-model:open="isCreateLoanOpen"/>
  </UContainer>
</template>
