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
import {AMOUNT_INPUT} from "~/utils/money";

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
const loading = ref(false);
const formId = useId();

const amountLabel = t('transactions.recurring.form.amount');
const descriptionLabel = t('transactions.recurring.form.description');

const schema = z.object({
  amount: z.coerce.number({message: t('common.validation.number', {field: amountLabel})})
    .min(AMOUNT_INPUT.min, t('common.validation.min', {field: amountLabel, min: AMOUNT_INPUT.min}))
    .max(AMOUNT_INPUT.max, t('common.validation.max', {field: amountLabel, max: AMOUNT_INPUT.max})),
  description: z.string().trim()
    .min(1, t('common.validation.required', {field: descriptionLabel}))
    .max(255, t('common.validation.maxLength', {field: descriptionLabel, max: 255})),
  frequency: z.nativeEnum(Frequency, {message: t('common.validation.required', {field: t('transactions.recurring.form.frequency')})}),
  startDate: z.string().min(1, t('common.validation.required', {field: t('transactions.recurring.form.startDate')})),
  isTransfer: z.boolean(),
  category: z.any().optional(),
  sourceAccountId: z.string().optional(),
  destinationAccountId: z.string().optional(),
}).superRefine((d, ctx) => {
  if (d.isTransfer) {
    if (!d.sourceAccountId) {
      ctx.addIssue({code: z.ZodIssueCode.custom, path: ['sourceAccountId'], message: t('common.validation.required', {field: t('transactions.transfer.fromAccount')})});
    }
    if (!d.destinationAccountId) {
      ctx.addIssue({code: z.ZodIssueCode.custom, path: ['destinationAccountId'], message: t('common.validation.required', {field: t('transactions.transfer.toAccount')})});
    }
  } else if (!d.category) {
    ctx.addIssue({code: z.ZodIssueCode.custom, path: ['category'], message: t('common.validation.required', {field: t('transactions.recurring.form.category')})});
  }
});
type Schema = z.output<typeof schema>;

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

async function handleSave(_event: FormSubmitEvent<Schema>) {
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
    await service.update(props.rule.id, {
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
      <UForm :id="formId" :schema="schema" :state="form" @submit="handleSave">
        <RecurringFormFields v-model="form"
                             :disabled="loading"
                             source-locked/>
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
