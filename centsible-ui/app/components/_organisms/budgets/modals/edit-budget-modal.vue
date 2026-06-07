<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import type {Budget, BudgetForm} from "~/models/budget/budget";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
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
const loading = ref(false);

const categoryLabel = t('budgets.form.categoryLabel');
const limitLabel = t('budgets.form.limitLabel');

const schema = z.object({
  category: z.any().refine((v) => !!v, t('common.validation.required', {field: categoryLabel})),
  amountLimit: z.coerce.number({message: t('common.validation.number', {field: limitLabel})})
    .min(0.01, t('common.validation.min', {field: limitLabel, min: 0.01}))
    .max(9999999.99, t('common.validation.max', {field: limitLabel, max: 9999999.99})),
})
type Schema = z.output<typeof schema>

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

async function handleSave(_event: FormSubmitEvent<Schema>) {
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
      <UForm id="edit-budget-form" :schema="schema" :state="form" @submit="handleSave">
        <BudgetFormFields v-model="form"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions form="edit-budget-form"
                          :loading="loading"
                          :submit-label="t('budgets.edit.submit')"
                          @cancel="isOpen = false"/>
    </template>
  </UModal>
</template>
