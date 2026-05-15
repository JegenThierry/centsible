<script lang="ts" setup>
import type {Budget, BudgetForm} from "~/models/budget/budget";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import BudgetFormFields from "~/components/_molecules/budgets/budget-form.vue";
import {useBudgetService} from "~/services/budget/budget-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";

const props = defineProps<{
  budget: Budget;
}>();
const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'updated'): void;
}>();

const service = useBudgetService(useApi());
const toasts = useToasts();

const form = ref<BudgetForm>(toForm(props.budget));
const formRef = ref<InstanceType<typeof BudgetFormFields>>();
const loading = ref(false);

function toForm(budget: Budget): BudgetForm {
  return {category: budget.category, amountLimit: budget.amountLimit};
}

watch(() => props.budget, (b) => {
  form.value = toForm(b);
});

async function handleSave() {
  if (!formRef.value?.validate()) return;
  if (!form.value.category?.id) return;

  loading.value = true;
  try {
    await service.update(props.budget.id, {
      categoryId: form.value.category.id,
      amountLimit: form.value.amountLimit,
    });
    toasts.success('Budget updated.', 'Your monthly limit has been saved.');
    emit('updated');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, 'Budget not updated.', 'Could not update the budget, please try again.');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          description="Update this budget's category or monthly limit."
          title="Edit budget">
    <template #body>
      <BudgetFormFields ref="formRef" v-model="form"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">Save</UButton>
      </div>
    </template>
  </UModal>
</template>
