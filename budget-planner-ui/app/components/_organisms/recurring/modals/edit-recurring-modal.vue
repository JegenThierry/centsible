<script lang="ts" setup>
import type {RecurringTransaction, RecurringTransactionForm} from "~/models/recurring/recurring-transaction";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import RecurringFormFields from "~/components/_molecules/recurring/recurring-form.vue";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";

const props = defineProps<{
  rule: RecurringTransaction;
}>();
const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'updated'): void;
}>();

const api = useApi();
const service = useRecurringTransactionService(api);
const toasts = useToasts();

const form = ref<RecurringTransactionForm>(toForm(props.rule));
const formRef = ref<InstanceType<typeof RecurringFormFields>>();
const loading = ref(false);

function toForm(rule: RecurringTransaction): RecurringTransactionForm {
  return {
    amount: rule.amount,
    description: rule.description,
    category: rule.category,
    frequency: rule.frequency,
    startDate: rule.startDate.split('T')[0],
    endDate: rule.endDate ? rule.endDate.split('T')[0] : undefined,
    active: rule.active,
  };
}

watch(() => props.rule, (rule) => {
  form.value = toForm(rule);
});

async function handleSave() {
  if (!formRef.value?.validate()) return;
  if (!form.value.category?.id || !form.value.startDate) return;

  loading.value = true;
  try {
    await service.update(props.rule.id, {
      amount: form.value.amount,
      description: form.value.description,
      categoryId: form.value.category.id,
      frequency: form.value.frequency,
      startDate: form.value.startDate,
      endDate: form.value.endDate || null,
      active: form.value.active,
    });
    toasts.success('Recurring rule updated.', 'Future occurrences will use the new values.');
    emit('updated');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, 'Rule not updated.', 'Could not update the recurring rule, please try again.');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          description="Update this recurring rule. Already-generated transactions are not changed."
          title="Edit recurring rule">
    <template #body>
      <RecurringFormFields ref="formRef" v-model="form"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">Save</UButton>
      </div>
    </template>
  </UModal>
</template>
