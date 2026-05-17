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
const {t} = useI18n();

const form = ref<BudgetForm>(toForm(props.budget));
const formRef = ref<InstanceType<typeof BudgetFormFields>>();
const loading = ref(false);

function toForm(budget: Budget): BudgetForm {
  return {
    category: budget.category,
    amountLimit: budget.amountLimit,
    periodType: budget.periodType ?? 'MONTHLY',
    rolloverEnabled: budget.rolloverEnabled ?? false,
  };
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
      periodType: form.value.periodType,
      rolloverEnabled: form.value.rolloverEnabled,
    });
    toasts.success(t('budgets.edit.toastSuccessTitle'), t('budgets.edit.toastSuccessBody'));
    emit('updated');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, t('budgets.edit.toastErrorTitle'), t('budgets.edit.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t('budgets.edit.description')"
          :title="t('budgets.edit.title')">
    <template #body>
      <BudgetFormFields ref="formRef" v-model="form"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">{{ t('budgets.edit.submit') }}</UButton>
      </div>
    </template>
  </UModal>
</template>
