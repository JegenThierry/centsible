<script lang="ts" setup>
import {Frequency, type RecurringTransactionForm} from "~/models/recurring/recurring-transaction";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import RecurringFormFields from "~/components/_molecules/recurring/recurring-form.vue";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {todayIsoDate} from "~/utils/date";

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const api = useApi();
const service = useRecurringTransactionService(api);
const toasts = useToasts();
const budgetAccountsStore = useBudgetAccountsStore();

const form = ref<RecurringTransactionForm>(makeBlankForm());
const formRef = ref<InstanceType<typeof RecurringFormFields>>();
const loading = ref(false);

function makeBlankForm(): RecurringTransactionForm {
  return {
    amount: 0,
    description: '',
    category: undefined,
    frequency: Frequency.MONTHLY,
    startDate: todayIsoDate(),
    endDate: undefined,
    active: true,
  };
}

watch(isOpen, (open) => {
  if (open) form.value = makeBlankForm();
});

async function handleSave() {
  if (!formRef.value?.validate()) return;
  if (!budgetAccountsStore.activeAccount?.id) return;
  if (!form.value.category?.id || !form.value.startDate) return;

  loading.value = true;
  try {
    await service.create(budgetAccountsStore.activeAccount.id, {
      amount: form.value.amount,
      description: form.value.description,
      categoryId: form.value.category.id,
      frequency: form.value.frequency,
      startDate: form.value.startDate,
      endDate: form.value.endDate || null,
      active: form.value.active,
    });
    toasts.success('Recurring rule created.', 'The next occurrence will be generated on its due date.');
    emit('created');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, 'Rule not created.', 'Could not create the recurring rule, please try again.');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          description="Schedule a transaction to be generated automatically."
          title="New recurring rule">
    <template #body>
      <RecurringFormFields ref="formRef" v-model="form"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">Create</UButton>
      </div>
    </template>
  </UModal>
</template>
