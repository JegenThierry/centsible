<script lang="ts" setup>
import {z} from 'zod';
import type {FormSubmitEvent} from '@nuxt/ui';
import {type Transaction, type TransactionForm} from "~/models/transactions/transaction";
import {resolveSplitPayload, transactionType} from "~/utils/transaction";
import {CategoryType} from "~/models/category/category";
import {Currency} from "~/models/budget-account/currency";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import TransactionFormFields from "~/components/_molecules/transactions/transaction-form.vue";
import TransactionAttachments from "~/components/_organisms/transactions/transaction-attachments.vue";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {useTagService} from "~/services/tag/tag-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useModalDirtyGuard} from "~/composables/use-unsaved-changes-guard";
import {useRuleSuggestions} from "~/composables/use-rule-suggestions";
import {todayIsoDate} from "~/utils/date";
import {transactionSchema} from "~/utils/form-schemas";

const props = defineProps<{
  transaction: Transaction;
}>();
const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'updated'): void;
}>();

const api = useApi();
const transactionService = useTransactionService(api);
const tagService = useTagService(api);
const toasts = useToasts();
const {toastError} = useApiErrors();
const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const form = ref<TransactionForm>({
  amount: 0,
  description: '',
  category: undefined,
  type: CategoryType.EXPENSE,
  transactionDate: todayIsoDate(),
  currency: Currency.EUR,
  tagIds: [],
});

const loading = ref(false);
const formId = useId();
const activeCurrency = computed(() => budgetAccountsStore.activeAccount?.currency);

const {
  reset: resetRuleSuggestions,
  markCategoryTouched,
  appliedRule: ruleHint,
} = useRuleSuggestions({
  enabled: isOpen,
  form,
  accountId: computed(() => budgetAccountsStore.activeAccount?.id),
});

// Same schema as the create modal's standard mode — see utils/form-schemas.
const schema = transactionSchema(t);
type Schema = z.output<typeof schema>;

function loadTransaction(transaction: Transaction) {
  form.value = {
    amount: transaction.originalAmount ?? transaction.amount,
    description: transaction.description,
    category: transaction.category,
    type: transactionType(transaction),
    transactionDate: transaction.transactionDate.split('T')[0],
    currency: transaction.originalCurrency ?? activeCurrency.value ?? Currency.EUR,
    tagIds: transaction.tags?.map((tg) => tg.id) ?? [],
    splits: transaction.splits && transaction.splits.length > 0
      ? transaction.splits.map((s) => ({category: s.category, amount: s.amount, note: s.note ?? undefined}))
      : undefined,
  };
}

const {requestClose} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => form.value,
  onResetOnOpen: () => {
    loadTransaction(props.transaction);
    resetRuleSuggestions();
  },
});

async function handleEdit(_event: FormSubmitEvent<Schema>) {
  if (loading.value) return;
  if (!budgetAccountsStore.activeAccount?.id) return;
  if (props.transaction.id === undefined) return;
  if (!form.value.category?.id || !form.value.transactionDate) return;
  const splitResolution = resolveSplitPayload(form.value);
  if (splitResolution.error) {
    toasts.error(t('transactions.form.split.invalidTitle'), t(`transactions.form.split.error.${splitResolution.error}`));
    return;
  }

  loading.value = true;
  try {
    await transactionService.updateTransaction(
      budgetAccountsStore.activeAccount.id,
      props.transaction.id,
      {
        amount: form.value.amount,
        description: form.value.description,
        categoryId: form.value.category.id,
        transactionDate: form.value.transactionDate,
        type: form.value.type,
        currency: form.value.currency,
        splits: splitResolution.splits,
      }
    );
    try {
      await tagService.setForTransaction(props.transaction.id, form.value.tagIds ?? []);
    } catch {
      toasts.error(t('transactions.form.tagsApplyFailedTitle'), t('transactions.form.tagsApplyFailedBody'));
    }
    emit('updated');
    toasts.success(t('transactions.edit.toastSuccessTitle'), t('transactions.edit.toastSuccessBody'));
    isOpen.value = false;
  } catch (error) {
    toastError(error, t('transactions.edit.toastErrorTitle'), t('transactions.edit.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}

</script>

<template>
  <UModal :open="isOpen"
          :description="t('transactions.edit.description')"
          :title="t('transactions.edit.title')"
          @update:open="requestClose">
    <template #body>
      <UForm :id="formId" :schema="schema" :state="form" @submit="handleEdit">
        <TransactionFormFields v-model="form"
                               :account-id="budgetAccountsStore.activeAccount?.id"
                               :account-currency="activeCurrency"
                               :disabled="loading"
                               :rule-hint="ruleHint"
                               @manual-category="markCategoryTouched"/>
      </UForm>
      <div class="mt-6 border-t border-default pt-4">
        <TransactionAttachments :transaction-id="transaction.id"/>
      </div>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="t('transactions.edit.submit')"
                          @cancel="requestClose(false)"/>
    </template>
  </UModal>
</template>
