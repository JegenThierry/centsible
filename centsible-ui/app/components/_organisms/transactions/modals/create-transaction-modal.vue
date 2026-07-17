<script lang="ts" setup>
import type {FormSubmitEvent} from '@nuxt/ui';
import {type SetBalanceForm as SetBalanceFormModel, type TransactionForm, type TransferForm as TransferFormModel} from "~/models/transactions/transaction";
import {CategorySystemKey, CategoryType} from "~/models/category/category";
import {Currency} from "~/models/budget-account/currency";
import type {LoanForm as LoanFormModel} from "~/models/loan/loan";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import TransactionFormFields from "~/components/_molecules/transactions/transaction-form.vue";
import TransferFormFields from "~/components/_molecules/transactions/transfer-form.vue";
import SetBalanceFormFields from "~/components/_molecules/transactions/set-balance-form.vue";
import TransactionAttachments from "~/components/_organisms/transactions/transaction-attachments.vue";
import LoanFormFields from "~/components/_organisms/loans/loan-form.vue";
import AppRadioGroup from "~/components/_atoms/ui/app-radio-group.vue";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {useTagService} from "~/services/tag/tag-service";
import {useLoansStore} from "~/stores/loansStore";
import {useCategoriesStore} from "~/stores/categoriesStore";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useModalDirtyGuard} from "~/composables/use-unsaved-changes-guard";
import {useRuleSuggestions} from "~/composables/use-rule-suggestions";
import {resolveSplitPayload} from "~/utils/transaction";
import {todayIsoDate} from "~/utils/date";
import {loanSchema, setBalanceSchema, transactionSchema, transferSchema} from "~/utils/form-schemas";

const {t} = useI18n();

const props = defineProps<{
  title?: string;
  description?: string;
  filterType?: CategoryType;
}>();

const computedTitle = computed(() => props.title ?? t('transactions.create.title'));
const computedDescription = computed(() => props.description ?? t('transactions.create.description'));

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const api = useApi();
const transactionService = useTransactionService(api);
const tagService = useTagService(api);
const loansStore = useLoansStore();
const categoriesStore = useCategoriesStore();
const toasts = useToasts();
const {toastError} = useApiErrors();
const budgetAccountsStore = useBudgetAccountsStore();

type Mode = 'standard' | 'transfer' | 'lending' | 'setBalance';
const mode = ref<Mode>('standard');

const modeOptions = computed(() => [
  {label: t('transactions.create.modeStandard'), value: 'standard'},
  {label: t('transactions.create.modeTransfer'), value: 'transfer'},
  {label: t('transactions.create.modeLending'), value: 'lending'},
  {label: t('transactions.create.modeSetBalance'), value: 'setBalance'},
]);

const attachmentsRef = ref<InstanceType<typeof TransactionAttachments>>();
const loading = ref(false);
const formId = useId();
const activeCurrency = computed(() => budgetAccountsStore.activeAccount?.currency);
const activeAccountBalance = computed(() => budgetAccountsStore.activeAccount?.balance ?? 0);

const schemaStd = transactionSchema(t);
const schemaTransfer = transferSchema(t);
const schemaSetBalance = setBalanceSchema(t);
const schemaLoan = loanSchema(t);

const balanceAdjustmentCategory = computed(() =>
  categoriesStore.categories.find(c => c.systemKey === CategorySystemKey.BalanceAdjustment),
);

const form = ref<TransactionForm>(makeBlankTransactionForm());
const loanForm = ref<LoanFormModel>(makeBlankLoanForm());
const setBalanceForm = ref<SetBalanceFormModel>(makeBlankSetBalanceForm());
const transferForm = ref<TransferFormModel>(makeBlankTransferForm());

const {
  reset: resetRuleSuggestions,
  markCategoryTouched,
  appliedRule: ruleHint,
} = useRuleSuggestions({
  enabled: computed(() => isOpen.value && mode.value === 'standard'),
  form,
  accountId: computed(() => budgetAccountsStore.activeAccount?.id),
});

const schema = computed(() => ({
  standard: schemaStd,
  transfer: schemaTransfer,
  lending: schemaLoan,
  setBalance: schemaSetBalance,
}[mode.value]));

const state = computed(() => ({
  standard: form.value as Record<string, unknown>,
  transfer: transferForm.value as Record<string, unknown>,
  lending: loanForm.value as Record<string, unknown>,
  setBalance: setBalanceForm.value as Record<string, unknown>,
}[mode.value]));

function makeBlankTransactionForm(): TransactionForm {
  return {
    amount: 0,
    description: '',
    category: undefined,
    type: props.filterType ?? CategoryType.EXPENSE,
    transactionDate: todayIsoDate(),
    currency: budgetAccountsStore.activeAccount?.currency ?? Currency.EUR,
    tagIds: [],
  };
}

function makeBlankLoanForm(): LoanFormModel {
  return {
    contactId: undefined,
    newContactFirstName: undefined,
    newContactLastName: undefined,
    accountId: budgetAccountsStore.activeAccount?.id,
    affectBalance: true,
    lentAmount: 0,
    owedAmount: 0,
    description: '',
    transactionDate: todayIsoDate(),
    dueDate: undefined,
    notes: undefined,
  };
}

function makeBlankSetBalanceForm(): SetBalanceFormModel {
  return {
    newBalance: activeAccountBalance.value,
    category: balanceAdjustmentCategory.value,
    description: '',
    transactionDate: todayIsoDate(),
  };
}

function makeBlankTransferForm(): TransferFormModel {
  return {
    amount: 0,
    sourceAccountId: budgetAccountsStore.activeAccount?.id,
    destinationAccountId: undefined,
    description: '',
    transactionDate: todayIsoDate(),
  };
}

watch(balanceAdjustmentCategory, (category) => {
  if (category && mode.value === 'setBalance' && !setBalanceForm.value.category) {
    setBalanceForm.value.category = category;
  }
});

function activeFormSnapshot() {
  if (mode.value === 'transfer') return transferForm.value;
  if (mode.value === 'lending') return loanForm.value;
  if (mode.value === 'setBalance') return setBalanceForm.value;
  return form.value;
}

const {requestClose} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => ({mode: mode.value, form: activeFormSnapshot()}),
  onResetOnOpen: () => {
    mode.value = 'standard';
    form.value = makeBlankTransactionForm();
    loanForm.value = makeBlankLoanForm();
    setBalanceForm.value = makeBlankSetBalanceForm();
    transferForm.value = makeBlankTransferForm();
    attachmentsRef.value?.clearPending();
    if (categoriesStore.categories.length === 0) categoriesStore.updateCategories();
    if (budgetAccountsStore.availableAccounts.length === 0) budgetAccountsStore.updateAvailableAccounts();
    resetRuleSuggestions();
  },
});

async function handleSave(_event: FormSubmitEvent<unknown>) {
  if (loading.value) return;
  if (mode.value === 'transfer') return saveTransfer();
  if (mode.value === 'lending') return saveLending();
  if (mode.value === 'setBalance') return saveSetBalance();
  return saveStandard();
}

async function saveTransfer() {
  const sourceId = transferForm.value.sourceAccountId;
  const destinationId = transferForm.value.destinationAccountId;
  if (!sourceId || !destinationId || !transferForm.value.transactionDate) return;
  if (sourceId === destinationId) {
    toasts.error(t('transactions.transfer.sameAccountTitle'), t('transactions.transfer.sameAccountBody'));
    return;
  }

  loading.value = true;
  try {
    await transactionService.createTransfer(sourceId, {
      amount: transferForm.value.amount,
      destinationAccountId: destinationId,
      description: transferForm.value.description,
      transactionDate: transferForm.value.transactionDate,
    });
    toasts.success(t('transactions.transfer.toastSuccessTitle'), t('transactions.transfer.toastSuccessBody'));
    emit('created');
    isOpen.value = false;
  } catch (error) {
    toastError(error, t('transactions.transfer.toastErrorTitle'), t('transactions.transfer.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}

async function saveStandard() {
  if (!budgetAccountsStore.activeAccount?.id) return;
  if (!form.value.category?.id || !form.value.transactionDate) return;
  const splitResolution = resolveSplitPayload(form.value);
  if (splitResolution.error) {
    toasts.error(t('transactions.form.split.invalidTitle'), t(`transactions.form.split.error.${splitResolution.error}`));
    return;
  }

  loading.value = true;
  try {
    const created = await transactionService.createTransaction(
      budgetAccountsStore.activeAccount.id,
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
    toasts.success(t('transactions.create.toastSuccessTitle'), t('transactions.create.toastSuccessBody'));

    const result = await attachmentsRef.value?.uploadPending(created.id);
    if (result && result.failed.length > 0) {
      toasts.error(
        t('transactions.attachments.errors.uploadPartialTitle'),
        t('transactions.attachments.errors.uploadPartialBody', {count: result.failed.length}),
      );
    }

    if (form.value.tagIds && form.value.tagIds.length > 0) {
      try {
        await tagService.setForTransaction(created.id, form.value.tagIds);
      } catch {
        toasts.error(t('transactions.form.tagsApplyFailedTitle'), t('transactions.form.tagsApplyFailedBody'));
      }
    }

    emit('created');
    isOpen.value = false;
  } catch (error) {
    toastError(error, t('transactions.create.toastErrorTitle'), t('transactions.create.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}

async function saveLending() {
  loading.value = true;
  try {
    await loansStore.createLoan(loanForm.value);
    emit('created');
    isOpen.value = false;
  } catch (error) {
    toastError(error, t('transactions.create.toastErrorTitle'), t('transactions.create.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}

async function saveSetBalance() {
  if (!budgetAccountsStore.activeAccount?.id) return;
  if (!setBalanceForm.value.category?.id || !setBalanceForm.value.transactionDate) return;

  loading.value = true;
  try {
    await transactionService.setAccountBalance(
      budgetAccountsStore.activeAccount.id,
      {
        newBalance: setBalanceForm.value.newBalance,
        categoryId: setBalanceForm.value.category.id,
        description: setBalanceForm.value.description,
        transactionDate: setBalanceForm.value.transactionDate,
      }
    );
    toasts.success(t('transactions.create.toastSuccessTitle'), t('transactions.create.toastSuccessBody'));
    emit('created');
    isOpen.value = false;
  } catch (error) {
    toastError(error, t('transactions.create.toastErrorTitle'), t('transactions.create.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal :open="isOpen"
          :description="computedDescription"
          :title="computedTitle"
          @update:open="requestClose">
    <template #body>
      <UForm :id="formId" :schema="schema" :state="state" class="space-y-4" @submit="handleSave">
        <AppRadioGroup v-model="mode"
                       :disabled="loading"
                       :items="modeOptions"
                       :legend="t('transactions.create.modeLegend')"
                       orientation="horizontal"/>
        <TransactionFormFields v-if="mode === 'standard'"
                               v-model="form"
                               :account-id="budgetAccountsStore.activeAccount?.id"
                               :account-currency="activeCurrency"
                               :disabled="loading"
                               :filter-type="filterType"
                               :rule-hint="ruleHint"
                               @manual-category="markCategoryTouched"/>
        <TransferFormFields v-else-if="mode === 'transfer'"
                            v-model="transferForm"
                            :accounts="budgetAccountsStore.availableAccounts"
                            :disabled="loading"/>
        <LoanFormFields v-else-if="mode === 'lending'"
                        v-model="loanForm"
                        :disabled="loading"/>
        <SetBalanceFormFields v-else
                              v-model="setBalanceForm"
                              :current-balance="activeAccountBalance"
                              :currency="activeCurrency"
                              :disabled="loading"/>
      </UForm>
      <div v-if="mode === 'standard'" class="mt-6 border-t border-default pt-4">
        <TransactionAttachments ref="attachmentsRef"/>
      </div>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="t('transactions.create.submit')"
                          @cancel="requestClose(false)"/>
    </template>
  </UModal>
</template>
