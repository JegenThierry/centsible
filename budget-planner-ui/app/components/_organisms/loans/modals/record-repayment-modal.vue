<script lang="ts" setup>
import type {Loan, RepaymentForm as RepaymentFormModel} from "~/models/loan/loan";
import RepaymentForm from "~/components/_molecules/loans/repayment-form.vue";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import {useLoansStore} from "~/stores/loansStore";
import {todayIsoDate} from "~/utils/date";

const props = defineProps<{
  loan: Loan | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'recorded'): void;
}>();

const loansStore = useLoansStore();

const form = ref<RepaymentFormModel>(makeBlankForm());
const formRef = ref<InstanceType<typeof RepaymentForm>>();
const loading = ref(false);

function makeBlankForm(): RepaymentFormModel {
  return {
    accountId: props.loan?.accountId,
    affectBalance: props.loan?.affectsBalance ?? true,
    amount: Number(props.loan?.outstanding ?? 0),
    description: '',
    repaidAt: todayIsoDate(),
  };
}

watch(isOpen, (open) => {
  if (open) form.value = makeBlankForm();
});

async function handleSave() {
  if (!props.loan?.id || !props.loan.contact.id) return;
  if (!formRef.value?.validate()) return;

  loading.value = true;
  try {
    await loansStore.recordRepayment(props.loan.id, props.loan.contact.id, form.value);
    emit('recorded');
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
          :description="loan ? `Record a repayment from ${loan.contact.name}.` : ''"
          title="Record Repayment">
    <template #body>
      <RepaymentForm v-if="loan" ref="formRef" v-model="form" :max-amount="Number(loan.outstanding)"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">Record</UButton>
      </div>
    </template>
  </UModal>
</template>
