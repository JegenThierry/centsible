<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import type {BudgetForm, BudgetPeriodType} from "~/models/budget/budget";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import BudgetFormFields from "~/components/_molecules/budgets/budget-form.vue";
import {useBudgetService} from "~/services/budget/budget-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {budgetSchema} from "~/utils/form-schemas";

const isOpen = defineModel<boolean>('open', {required: true});

defineProps<{
  /** (category, period) pairs already budgeted — forwarded to the form to exclude duplicates. */
  existingCombos?: Array<{categoryId: number; periodType: BudgetPeriodType}>;
}>();

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const service = useBudgetService(useApi());
const toasts = useToasts();
const {t} = useI18n();

const form = ref<BudgetForm>(makeBlank());
const loading = ref(false);

const schema = budgetSchema(t);
type Schema = z.output<typeof schema>

function makeBlank(): BudgetForm {
  return {category: undefined, amountLimit: 0, periodType: 'MONTHLY', rolloverEnabled: false};
}

watch(isOpen, (open) => {
  if (open) form.value = makeBlank();
});

async function handleSave(_event: FormSubmitEvent<Schema>) {
  if (!form.value.category?.id) return;

  loading.value = true;
  try {
    await service.create({
      categoryId: form.value.category.id,
      amountLimit: form.value.amountLimit,
      periodType: form.value.periodType,
      rolloverEnabled: form.value.rolloverEnabled,
    });
    toasts.success(t('budgets.create.toastSuccessTitle'), t('budgets.create.toastSuccessBody'));
    emit('created');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, t('budgets.create.toastErrorTitle'), t('budgets.create.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t('budgets.create.description')"
          :title="t('budgets.create.title')">
    <template #body>
      <UForm id="create-budget-form" :schema="schema" :state="form" @submit="handleSave">
        <BudgetFormFields v-model="form" :existing-combos="existingCombos"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions form="create-budget-form"
                          :loading="loading"
                          :submit-label="t('budgets.create.submit')"
                          @cancel="isOpen = false"/>
    </template>
  </UModal>
</template>
