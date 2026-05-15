<script lang="ts" setup>
import type {BudgetForm} from "~/models/budget/budget";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import BudgetFormFields from "~/components/_molecules/budgets/budget-form.vue";
import {useBudgetService} from "~/services/budget/budget-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const service = useBudgetService(useApi());
const toasts = useToasts();

const form = ref<BudgetForm>(makeBlank());
const formRef = ref<InstanceType<typeof BudgetFormFields>>();
const loading = ref(false);

function makeBlank(): BudgetForm {
  return {category: undefined, amountLimit: 0};
}

watch(isOpen, (open) => {
  if (open) form.value = makeBlank();
});

async function handleSave() {
  if (!formRef.value?.validate()) return;
  if (!form.value.category?.id) return;

  loading.value = true;
  try {
    await service.create({
      categoryId: form.value.category.id,
      amountLimit: form.value.amountLimit,
    });
    toasts.success('Budget created.', 'Track your spending against this limit each month.');
    emit('created');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, 'Budget not created.', 'Could not create the budget. A budget for this category may already exist.');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          description="Set a monthly spending limit for an expense category."
          title="New budget">
    <template #body>
      <BudgetFormFields ref="formRef" v-model="form"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">Create</UButton>
      </div>
    </template>
  </UModal>
</template>
