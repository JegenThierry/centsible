<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import type {Budget, BudgetForm, BudgetPeriodType} from "~/models/budget/budget";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import BudgetFormFields from "~/components/_molecules/budgets/budget-form.vue";
import {useBudgetService} from "~/services/budget/budget-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {budgetSchema} from "~/utils/form-schemas";

const props = defineProps<{
  /** Present → edit that budget; absent → create a new one. */
  budget?: Budget;
  /** (category, period) pairs already budgeted — forwarded to the form to exclude duplicates (create). */
  existingCombos?: Array<{categoryId: number; periodType: BudgetPeriodType}>;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'created'): void;
  (e: 'updated'): void;
}>();

const service = useBudgetService(useApi());
const toasts = useToasts();
const {t} = useI18n();

const isEdit = computed(() => !!props.budget);

const form = ref<BudgetForm>(props.budget ? toForm(props.budget) : makeBlank());
const loading = ref(false);
const formId = useId();

const schema = budgetSchema(t);
type Schema = z.output<typeof schema>

function makeBlank(): BudgetForm {
  return {category: undefined, amountLimit: 0, periodType: 'MONTHLY', rolloverEnabled: false};
}

function toForm(budget: Budget): BudgetForm {
  return {
    category: budget.category,
    amountLimit: budget.amountLimit,
    periodType: budget.periodType ?? 'MONTHLY',
    rolloverEnabled: budget.rolloverEnabled ?? false,
  };
}

function syncForm() {
  form.value = props.budget ? toForm(props.budget) : makeBlank();
}

watch(isOpen, (open) => {
  if (open) syncForm();
});
watch(() => props.budget, () => {
  if (isOpen.value) syncForm();
});

async function handleSave(_event: FormSubmitEvent<Schema>) {
  if (!form.value.category?.id) return;

  loading.value = true;
  try {
    const payload = {
      categoryId: form.value.category.id,
      amountLimit: form.value.amountLimit,
      periodType: form.value.periodType,
      rolloverEnabled: form.value.rolloverEnabled,
    };
    if (props.budget) {
      await service.update(props.budget.id, payload);
      toasts.success(t('budgets.edit.toastSuccessTitle'), t('budgets.edit.toastSuccessBody'));
      emit('updated');
    } else {
      await service.create(payload);
      toasts.success(t('budgets.create.toastSuccessTitle'), t('budgets.create.toastSuccessBody'));
      emit('created');
    }
    isOpen.value = false;
  } catch (error) {
    const phase = props.budget ? 'edit' : 'create';
    useApiErrors().toastError(error, t(`budgets.${phase}.toastErrorTitle`), t(`budgets.${phase}.toastErrorBody`));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t(isEdit ? 'budgets.edit.description' : 'budgets.create.description')"
          :title="t(isEdit ? 'budgets.edit.title' : 'budgets.create.title')">
    <template #body>
      <UForm :id="formId" :schema="schema" :state="form" @submit="handleSave">
        <BudgetFormFields v-model="form" :existing-combos="existingCombos"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="t(isEdit ? 'budgets.edit.submit' : 'budgets.create.submit')"
                          @cancel="isOpen = false"/>
    </template>
  </UModal>
</template>
