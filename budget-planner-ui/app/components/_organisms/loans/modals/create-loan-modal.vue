<script lang="ts" setup>
import type {LoanForm as LoanFormModel} from "~/models/loan/loan";
import LoanForm from "~/components/_molecules/loans/loan-form.vue";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import {useLoansStore} from "~/stores/loansStore";
import {todayIsoDate} from "~/utils/date";

const props = defineProps<{
  contactId?: string;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const loansStore = useLoansStore();

const form = ref<LoanFormModel>(makeBlankForm());
const formRef = ref<InstanceType<typeof LoanForm>>();
const loading = ref(false);

function makeBlankForm(): LoanFormModel {
  return {
    contactId: props.contactId,
    newContactFirstName: undefined,
    newContactLastName: undefined,
    accountId: undefined,
    affectBalance: true,
    lentAmount: 0,
    owedAmount: 0,
    description: '',
    transactionDate: todayIsoDate(),
    dueDate: undefined,
    notes: undefined,
  };
}

watch(isOpen, (open) => {
  if (open) form.value = makeBlankForm();
});

async function handleSave() {
  if (!formRef.value?.validate()) return;

  loading.value = true;
  try {
    await loansStore.createLoan(form.value);
    emit('created');
    isOpen.value = false;
  } catch {
    // toast handled by store
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          description="Record money you've lent to someone."
          title="Record Lending">
    <template #body>
      <LoanForm ref="formRef" v-model="form" :lock-contact="!!contactId"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">Record</UButton>
      </div>
    </template>
  </UModal>
</template>
