<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import {Frequency, type RecurringTransactionForm} from "~/models/recurring/recurring-transaction";
import {Currency} from "~/models/budget-account/currency";
import {CategoryType} from "~/models/category/category";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import RecurringFormFields from "~/components/_molecules/recurring/recurring-form.vue";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useModalDirtyGuard} from "~/composables/use-unsaved-changes-guard";
import {recurringSchema} from "~/utils/form-schemas";
import {todayIsoDate} from "~/utils/date";

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const api = useApi();
const service = useRecurringTransactionService(api);
const toasts = useToasts();
const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const form = ref<RecurringTransactionForm>(makeBlankForm());
const loading = ref(false);
const formId = useId();

const schema = recurringSchema(t);
type Schema = z.output<typeof schema>;

function makeBlankForm(): RecurringTransactionForm {
  return {
    amount: 0,
    description: '',
    category: undefined,
    frequency: Frequency.MONTHLY,
    startDate: todayIsoDate(),
    endDate: undefined,
    active: true,
    currency: budgetAccountsStore.activeAccount?.currency ?? Currency.EUR,
    type: CategoryType.EXPENSE,
    isTransfer: false,
    sourceAccountId: budgetAccountsStore.activeAccount?.id,
    destinationAccountId: undefined,
  };
}

const {requestClose} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => form.value,
  onResetOnOpen: () => { form.value = makeBlankForm(); },
});

async function handleSave(_event: FormSubmitEvent<Schema>) {
  if (loading.value) return;
  if (!form.value.startDate) return;

  const sourceAccountId = form.value.isTransfer
    ? form.value.sourceAccountId
    : budgetAccountsStore.activeAccount?.id;
  if (!sourceAccountId) return;

  if (form.value.isTransfer) {
    if (!form.value.destinationAccountId) return;
    if (sourceAccountId === form.value.destinationAccountId) {
      toasts.error(t('transactions.transfer.sameAccountTitle'), t('transactions.transfer.sameAccountBody'));
      return;
    }
  } else if (!form.value.category?.id) {
    return;
  }

  loading.value = true;
  try {
    await service.create(sourceAccountId, {
      amount: form.value.amount,
      description: form.value.description,
      categoryId: form.value.isTransfer ? null : form.value.category?.id,
      frequency: form.value.frequency,
      startDate: form.value.startDate,
      endDate: form.value.endDate || null,
      active: form.value.active,
      currency: form.value.isTransfer ? undefined : form.value.currency,
      type: form.value.isTransfer ? null : form.value.type,
      isTransfer: form.value.isTransfer,
      destinationAccountId: form.value.isTransfer ? form.value.destinationAccountId : null,
    });
    toasts.success(t('transactions.recurring.create.toastSuccessTitle'), t('transactions.recurring.create.toastSuccessBody'));
    emit('created');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, t('transactions.recurring.create.toastErrorTitle'), t('transactions.recurring.create.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal :open="isOpen"
          :description="t('transactions.recurring.create.description')"
          :title="t('transactions.recurring.create.title')"
          @update:open="requestClose">
    <template #body>
      <UForm :id="formId" :schema="schema" :state="form" @submit="handleSave">
        <RecurringFormFields v-model="form"
                             :disabled="loading"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="t('transactions.recurring.create.submit')"
                          @cancel="requestClose(false)"/>
    </template>
  </UModal>
</template>
