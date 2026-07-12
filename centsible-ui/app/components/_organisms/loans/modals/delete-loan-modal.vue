<script lang="ts" setup>
import ConfirmationModal from "~/components/_organisms/modals/confirmation-modal.vue";
import type {Loan} from "~/models/loan/loan";
import {useLoansStore} from "~/stores/loansStore";

const props = defineProps<{
  loan: Loan | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'deleted'): void;
}>();

const loansStore = useLoansStore();
const {t} = useI18n();

async function deleteLoan() {
  if (!props.loan?.id || !props.loan.contact.id) return;
  await loansStore.deleteLoan(props.loan.id, props.loan.contact.id);
  emit('deleted');
}
</script>

<template>
  <ConfirmationModal v-model:open="isOpen"
                     :title="t('contacts.loans.delete.title')"
                     :body="t('contacts.loans.delete.description')"
                     :confirm-label="t('contacts.loans.delete.submit')"
                     :manage-toasts="false"
                     :delete-callback="deleteLoan"/>
</template>
