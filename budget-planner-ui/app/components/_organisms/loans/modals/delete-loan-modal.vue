<script lang="ts" setup>
import type {Loan} from "~/models/loan/loan";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import {useLoansStore} from "~/stores/loansStore";

const props = defineProps<{
  loan: Loan | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'deleted'): void;
}>();

const loansStore = useLoansStore();
const loading = ref(false);

async function handleDelete() {
  if (!props.loan?.id || !props.loan.contact.id) return;

  loading.value = true;
  try {
    await loansStore.deleteLoan(props.loan.id, props.loan.contact.id);
    isOpen.value = false;
    emit('deleted');
  } catch (error) {
    // toast handled by store
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          description="This will delete the loan, its originating transaction, and all repayments. This cannot be undone."
          title="Delete Loan">
    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" color="error" @click="handleDelete">Delete</UButton>
      </div>
    </template>
  </UModal>
</template>
