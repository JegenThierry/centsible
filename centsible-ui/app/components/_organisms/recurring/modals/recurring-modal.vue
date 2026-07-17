<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import {Frequency, type RecurringTransaction, type RecurringTransactionForm} from "~/models/recurring/recurring-transaction";
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

const props = defineProps<{
  /** Present → edit that rule; absent → create a new one. */
  rule?: RecurringTransaction;
  /** Optional starting values for a NEW rule (e.g. seeded from an existing transaction). */
  seed?: RecurringTransactionForm;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'created'): void;
  (e: 'updated'): void;
}>();

const api = useApi();
const service = useRecurringTransactionService(api);
const toasts = useToasts();
const {toastError} = useApiErrors();
const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const isEdit = computed(() => !!props.rule);

const form = ref<RecurringTransactionForm>(props.rule ? toForm(props.rule) : (props.seed ?? makeBlankForm()));
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

function toForm(rule: RecurringTransaction): RecurringTransactionForm {
  return {
    amount: rule.originalAmount ?? rule.amount,
    description: rule.description,
    category: rule.isTransfer ? undefined : rule.category,
    frequency: rule.frequency,
    startDate: rule.startDate.split('T')[0],
    endDate: rule.endDate ? rule.endDate.split('T')[0] : undefined,
    active: rule.active,
    currency: rule.originalCurrency ?? budgetAccountsStore.activeAccount?.currency ?? Currency.EUR,
    type: rule.type ?? rule.category?.type ?? CategoryType.EXPENSE,
    isTransfer: rule.isTransfer,
    sourceAccountId: rule.accountId,
    destinationAccountId: rule.destinationAccountId ?? undefined,
  };
}

/** Request payload — identical for create and update. */
function buildPayload() {
  return {
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
  };
}

const {requestClose, captureSnapshot} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => form.value,
  onResetOnOpen: () => { form.value = props.rule ? toForm(props.rule) : (props.seed ?? makeBlankForm()); },
});

watch(() => props.rule, (rule) => {
  if (!rule) return;
  form.value = toForm(rule);
  nextTick(captureSnapshot);
});

function handleSave(_event: FormSubmitEvent<Schema>) {
  return props.rule ? saveEdit(props.rule) : saveCreate();
}

async function saveCreate() {
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
    await service.create(sourceAccountId, buildPayload());
    toasts.success(t('transactions.recurring.create.toastSuccessTitle'), t('transactions.recurring.create.toastSuccessBody'));
    emit('created');
    isOpen.value = false;
  } catch (error) {
    toastError(error, t('transactions.recurring.create.toastErrorTitle'), t('transactions.recurring.create.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}

async function saveEdit(rule: RecurringTransaction) {
  if (loading.value) return;
  if (!form.value.startDate) return;

  if (form.value.isTransfer) {
    if (!form.value.destinationAccountId || !form.value.sourceAccountId) return;
    if (form.value.sourceAccountId === form.value.destinationAccountId) {
      toasts.error(t('transactions.transfer.sameAccountTitle'), t('transactions.transfer.sameAccountBody'));
      return;
    }
  } else if (!form.value.category?.id) {
    return;
  }

  loading.value = true;
  try {
    await service.update(rule.id, buildPayload());
    toasts.success(t('transactions.recurring.edit.toastSuccessTitle'), t('transactions.recurring.edit.toastSuccessBody'));
    emit('updated');
    isOpen.value = false;
  } catch (error) {
    toastError(error, t('transactions.recurring.edit.toastErrorTitle'), t('transactions.recurring.edit.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal :open="isOpen"
          :description="t(isEdit ? 'transactions.recurring.edit.description' : 'transactions.recurring.create.description')"
          :title="t(isEdit ? 'transactions.recurring.edit.title' : 'transactions.recurring.create.title')"
          @update:open="requestClose">
    <template #body>
      <UForm :id="formId" :schema="schema" :state="form" @submit="handleSave">
        <RecurringFormFields v-model="form"
                             :disabled="loading"
                             :source-locked="isEdit"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="t(isEdit ? 'transactions.recurring.edit.submit' : 'transactions.recurring.create.submit')"
                          @cancel="requestClose(false)"/>
    </template>
  </UModal>
</template>
