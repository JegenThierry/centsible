<script lang="ts" setup>
import type {Budget, BudgetForm, BudgetPeriodType, BudgetRequest} from "~/models/budget/budget";
import BudgetFormFields from "~/components/_molecules/budgets/budget-form.vue";
import FormModal from "~/components/_molecules/modals/form-modal.vue";
import {useBudgetService} from "~/services/budget/budget-service";
import {useBudgetsStore} from "~/stores/budgetsStore";
import {useModalSubmit} from "~/composables/use-modal-submit";
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
const store = useBudgetsStore();
const {t} = useI18n();

const isEdit = computed(() => !!props.budget);

const form = ref<BudgetForm>(props.budget ? toForm(props.budget) : makeBlank());

const schema = budgetSchema(t);

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

function snapshot() {
  return form.value;
}

watch(() => props.budget, () => {
  if (isOpen.value) syncForm();
});

onMounted(() => {
  if (!isEdit.value) store.fetchSuggestions();
});

const {pending, submit} = useModalSubmit({
  open: isOpen,
  action: () => {
    const payload: BudgetRequest = {
      categoryId: form.value.category!.id,
      amountLimit: form.value.amountLimit,
      periodType: form.value.periodType,
      rolloverEnabled: form.value.rolloverEnabled,
    };
    return props.budget ? service.update(props.budget.id, payload) : service.create(payload);
  },
  successTitle: isEdit.value ? t('budgets.edit.toastSuccessTitle') : t('budgets.create.toastSuccessTitle'),
  successBody: isEdit.value ? t('budgets.edit.toastSuccessBody') : t('budgets.create.toastSuccessBody'),
  errorTitle: isEdit.value ? t('budgets.edit.toastErrorTitle') : t('budgets.create.toastErrorTitle'),
  errorBody: isEdit.value ? t('budgets.edit.toastErrorBody') : t('budgets.create.toastErrorBody'),
  onSuccess: () => {
    if (props.budget) emit('updated');
    else emit('created');
  },
});
</script>

<template>
  <FormModal v-model="isOpen"
             :description="t(isEdit ? 'budgets.edit.description' : 'budgets.create.description')"
             :title="t(isEdit ? 'budgets.edit.title' : 'budgets.create.title')"
             :schema="schema"
             :state="form"
             :loading="pending"
             :get-snapshot="snapshot"
             :on-reset-on-open="syncForm"
             :submit-label="t(isEdit ? 'budgets.edit.submit' : 'budgets.create.submit')"
             @submit="submit">
    <template #fields>
      <BudgetFormFields v-model="form"
                        :existing-combos="existingCombos"
                        :suggestions="isEdit ? [] : store.suggestions"/>
    </template>
  </FormModal>
</template>
