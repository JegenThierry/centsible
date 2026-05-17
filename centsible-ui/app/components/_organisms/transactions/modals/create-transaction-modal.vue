<script lang="ts" setup>
import {type TransactionForm} from "~/models/transactions/transaction";
import {CategoryType} from "~/models/category/category";
import type {LoanForm as LoanFormModel} from "~/models/loan/loan";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import TransactionFormFields from "~/components/_molecules/transactions/transaction-form.vue";
import LoanFormFields from "~/components/_molecules/loans/loan-form.vue";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {useLoansStore} from "~/stores/loansStore";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useModalDirtyGuard} from "~/composables/use-unsaved-changes-guard";
import {todayIsoDate} from "~/utils/date";

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
const loansStore = useLoansStore();
const toasts = useToasts();
const budgetAccountsStore = useBudgetAccountsStore();

const mode = ref<'standard' | 'lending'>('standard');

const modeOptions = computed(() => [
  {label: t('transactions.create.modeStandard'), value: 'standard'},
  {label: t('transactions.create.modeLending'), value: 'lending'},
]);

const form = ref<TransactionForm>(makeBlankTransactionForm());
const loanForm = ref<LoanFormModel>(makeBlankLoanForm());

const formRef = ref<InstanceType<typeof TransactionFormFields>>();
const loanFormRef = ref<InstanceType<typeof LoanFormFields>>();
const loading = ref(false);
const formId = useId();
const activeCurrency = computed(() => budgetAccountsStore.activeAccount?.currency);

function makeBlankTransactionForm(): TransactionForm {
  return {
    amount: 0,
    description: '',
    category: undefined,
    transactionDate: todayIsoDate(),
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

const {requestClose} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => ({mode: mode.value, form: form.value, loanForm: loanForm.value}),
  onResetOnOpen: () => {
    mode.value = 'standard';
    form.value = makeBlankTransactionForm();
    loanForm.value = makeBlankLoanForm();
  },
});

async function handleSave() {
  if (loading.value) return;
  await (mode.value === 'lending' ? saveLending() : saveStandard());
}

async function saveStandard() {
  if (!formRef.value?.validate()) return;
  if (!budgetAccountsStore.activeAccount?.id) return;
  if (!form.value.category?.id || !form.value.transactionDate) return;

  loading.value = true;
  try {
    await transactionService.createTransaction(
      budgetAccountsStore.activeAccount.id,
      {
        amount: form.value.amount,
        description: form.value.description,
        categoryId: form.value.category.id,
        transactionDate: form.value.transactionDate
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

async function saveLending() {
  if (!loanFormRef.value?.validate()) return;

  loading.value = true;
  try {
    await loansStore.createLoan(loanForm.value);
    emit('created');
    isOpen.value = false;
  } catch (error) {
    console.error('Create lending transaction failed', error);
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
      <UForm :id="formId" :state="form" class="space-y-4" @submit="handleSave">
        <URadioGroup v-model="mode"
                     :disabled="loading"
                     :items="modeOptions"
                     :legend="t('transactions.create.modeLegend')"
                     orientation="horizontal"/>
        <TransactionFormFields v-if="mode === 'standard'"
                               ref="formRef"
                               v-model="form"
                               :currency="activeCurrency"
                               :disabled="loading"
                               :filter-type="filterType"/>
        <LoanFormFields v-else
                        ref="loanFormRef"
                        v-model="loanForm"
                        :disabled="loading"/>
      </UForm>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton :disabled="loading" @click="requestClose(false)"/>
        <UButton :form="formId" :loading="loading" type="submit">{{ t('transactions.create.submit') }}</UButton>
      </div>
    </template>
  </UModal>
</template>
