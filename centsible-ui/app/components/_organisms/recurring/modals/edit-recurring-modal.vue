<script lang="ts" setup>
import type {RecurringTransaction, RecurringTransactionForm} from "~/models/recurring/recurring-transaction";
import {Currency} from "~/models/budget-account/currency";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import RecurringFormFields from "~/components/_molecules/recurring/recurring-form.vue";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useModalDirtyGuard} from "~/composables/use-unsaved-changes-guard";

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
const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const form = ref<RecurringTransactionForm>(toForm(props.rule));
const formRef = ref<InstanceType<typeof RecurringFormFields>>();
const loading = ref(false);
const formId = useId();

function toForm(rule: RecurringTransaction): RecurringTransactionForm {
  return {
    amount: rule.originalAmount ?? rule.amount,
    description: rule.description,
    category: rule.category,
    frequency: rule.frequency,
    startDate: rule.startDate.split('T')[0],
    endDate: rule.endDate ? rule.endDate.split('T')[0] : undefined,
    active: rule.active,
    currency: rule.originalCurrency ?? budgetAccountsStore.activeAccount?.currency ?? Currency.EUR,
  };
}

const {requestClose, captureSnapshot} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => form.value,
  onResetOnOpen: () => { form.value = toForm(props.rule); },
});

watch(() => props.rule, (rule) => {
  form.value = toForm(rule);
  nextTick(captureSnapshot);
});

async function handleSave() {
  if (loading.value) return;
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
      currency: form.value.currency,
    });
    toasts.success(t('transactions.recurring.edit.toastSuccessTitle'), t('transactions.recurring.edit.toastSuccessBody'));
    emit('updated');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, t('transactions.recurring.edit.toastErrorTitle'), t('transactions.recurring.edit.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal :open="isOpen"
          :description="t('transactions.recurring.edit.description')"
          :title="t('transactions.recurring.edit.title')"
          @update:open="requestClose">
    <template #body>
      <UForm :id="formId" :state="form" @submit="handleSave">
        <RecurringFormFields ref="formRef"
                             v-model="form"
                             :disabled="loading"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="t('transactions.recurring.edit.submit')"
                          @cancel="requestClose(false)"/>
    </template>
  </UModal>
</template>
