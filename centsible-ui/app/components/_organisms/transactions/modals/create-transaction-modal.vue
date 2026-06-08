<script lang="ts" setup>
import {z} from 'zod';
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
import {resolveSplitPayload} from "~/utils/transaction";
import {todayIsoDate} from "~/utils/date";
import {AMOUNT_INPUT, BALANCE_INPUT} from "~/utils/money";

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

const optionalNumber = (min: number, max: number, label: string) =>
  z.preprocess(
    (v) => (v === '' || v === undefined || v === null) ? undefined : v,
    z.coerce.number({message: t('common.validation.number', {field: label})})
      .min(min, t('common.validation.min', {field: label, min}))
      .max(max, t('common.validation.max', {field: label, max}))
      .optional(),
  );

const schemaStd = z.object({
  category: z.custom((v) => v != null && typeof v === 'object', {message: t('common.validation.required', {field: t('transactions.form.category')})}),
  amount: z.coerce.number({message: t('common.validation.number', {field: t('transactions.form.amount')})})
    .min(AMOUNT_INPUT.min, t('common.validation.min', {field: t('transactions.form.amount'), min: AMOUNT_INPUT.min}))
    .max(AMOUNT_INPUT.max, t('common.validation.max', {field: t('transactions.form.amount'), max: AMOUNT_INPUT.max})),
  description: z.string().trim().min(1, t('common.validation.required', {field: t('transactions.form.description')}))
    .max(255, t('common.validation.maxLength', {field: t('transactions.form.description'), max: 255})),
  transactionDate: z.string().min(1, t('common.validation.required', {field: t('transactions.form.date')})),
});

const schemaTransfer = z.object({
  sourceAccountId: z.string({message: t('common.validation.required', {field: t('transactions.transfer.fromAccount')})})
    .min(1, t('common.validation.required', {field: t('transactions.transfer.fromAccount')})),
  destinationAccountId: z.string({message: t('common.validation.required', {field: t('transactions.transfer.toAccount')})})
    .min(1, t('common.validation.required', {field: t('transactions.transfer.toAccount')})),
  amount: z.coerce.number({message: t('common.validation.number', {field: t('transactions.transfer.amount')})})
    .min(AMOUNT_INPUT.min, t('common.validation.min', {field: t('transactions.transfer.amount'), min: AMOUNT_INPUT.min}))
    .max(AMOUNT_INPUT.max, t('common.validation.max', {field: t('transactions.transfer.amount'), max: AMOUNT_INPUT.max})),
  description: z.string().trim().min(1, t('common.validation.required', {field: t('transactions.form.description')}))
    .max(255, t('common.validation.maxLength', {field: t('transactions.form.description'), max: 255})),
  transactionDate: z.string().min(1, t('common.validation.required', {field: t('transactions.form.date')})),
});

const schemaSetBalance = z.object({
  newBalance: z.coerce.number({message: t('common.validation.number', {field: t('transactions.form.modes.setBalance.newBalance')})})
    .min(BALANCE_INPUT.min, t('common.validation.min', {field: t('transactions.form.modes.setBalance.newBalance'), min: BALANCE_INPUT.min}))
    .max(BALANCE_INPUT.max, t('common.validation.max', {field: t('transactions.form.modes.setBalance.newBalance'), max: BALANCE_INPUT.max})),
  category: z.custom((v) => v != null && typeof v === 'object', {message: t('common.validation.required', {field: t('transactions.form.category')})}),
  description: z.string().trim().min(1, t('common.validation.required', {field: t('transactions.form.description')}))
    .max(255, t('common.validation.maxLength', {field: t('transactions.form.description'), max: 255})),
  transactionDate: z.string().min(1, t('common.validation.required', {field: t('transactions.form.date')})),
});

const contactLabel = computed(() => t('contacts.loans.form.pickContact'));
const accountLabel = computed(() => t('contacts.loans.form.fromAccountLabel'));

const schemaLoan = z.object({
  contactId: z.string().optional(),
  newContactFirstName: z.string().max(100, t('common.validation.maxLength', {field: t('contacts.loans.form.newFirstNameLabel'), max: 100})).optional(),
  newContactLastName: z.string().max(100, t('common.validation.maxLength', {field: t('contacts.loans.form.newLastNameLabel'), max: 100})).optional(),
  accountId: z.string().optional(),
  affectBalance: z.boolean(),
  lentAmount: z.coerce.number({message: t('common.validation.number', {field: t('contacts.loans.form.lentLabel')})})
    .min(0.01, t('common.validation.min', {field: t('contacts.loans.form.lentLabel'), min: 0.01}))
    .max(9999999.99, t('common.validation.max', {field: t('contacts.loans.form.lentLabel'), max: 9999999.99})),
  owedAmount: z.coerce.number({message: t('common.validation.number', {field: t('contacts.loans.form.owedLabel')})})
    .min(0, t('common.validation.min', {field: t('contacts.loans.form.owedLabel'), min: 0}))
    .max(9999999.99, t('common.validation.max', {field: t('contacts.loans.form.owedLabel'), max: 9999999.99})),
  interestRate: optionalNumber(0, 999.99, t('contacts.loans.form.interestRateLabel')),
  description: z.string().trim().min(1, t('common.validation.required', {field: t('contacts.loans.form.descriptionLabel')}))
    .max(255, t('common.validation.maxLength', {field: t('contacts.loans.form.descriptionLabel'), max: 255})),
  transactionDate: z.string().min(1, t('common.validation.required', {field: t('contacts.loans.form.dateLabel')})),
  dueDate: z.string().optional(),
  notes: z.string().max(500, t('common.validation.maxLength', {field: t('contacts.loans.form.notesLabel'), max: 500})).optional(),
})
  .refine((d) => !!d.contactId || !!d.newContactFirstName?.trim(), {
    message: t('common.validation.required', {field: contactLabel.value}),
    path: ['contactId'],
  })
  .refine((d) => !d.affectBalance || !!d.accountId, {
    message: t('common.validation.required', {field: accountLabel.value}),
    path: ['accountId'],
  });

const balanceAdjustmentCategory = computed(() =>
  categoriesStore.categories.find(c => c.systemKey === CategorySystemKey.BalanceAdjustment),
);

const form = ref<TransactionForm>(makeBlankTransactionForm());
const loanForm = ref<LoanFormModel>(makeBlankLoanForm());
const setBalanceForm = ref<SetBalanceFormModel>(makeBlankSetBalanceForm());
const transferForm = ref<TransferFormModel>(makeBlankTransferForm());

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
    useApiErrors().toastError(error, t('transactions.transfer.toastErrorTitle'), t('transactions.transfer.toastErrorBody'));
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
    useApiErrors().toastError(error, t('transactions.create.toastErrorTitle'), t('transactions.create.toastErrorBody'));
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
    useApiErrors().toastError(error, t('transactions.create.toastErrorTitle'), t('transactions.create.toastErrorBody'));
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
    useApiErrors().toastError(error, t('transactions.create.toastErrorTitle'), t('transactions.create.toastErrorBody'));
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
                               :filter-type="filterType"/>
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
