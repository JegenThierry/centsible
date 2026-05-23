<script lang="ts" setup>
import type {Loan} from "~/models/loan/loan";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
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
const loading = ref(false);

async function handleDelete() {
  if (!props.loan?.id || !props.loan.contact.id) return;

  loading.value = true;
  try {
    await loansStore.deleteLoan(props.loan.id, props.loan.contact.id);
    isOpen.value = false;
    emit('deleted');
  } catch (error) {
    console.error('Delete loan failed', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t('contacts.loans.delete.description')"
          :title="t('contacts.loans.delete.title')">
    <template #footer>
      <ModalFooterActions :loading="loading"
                          :submit-label="t('contacts.loans.delete.submit')"
                          submit-color="error"
                          @cancel="isOpen = false"
                          @submit="handleDelete"/>
    </template>
  </UModal>
</template>
