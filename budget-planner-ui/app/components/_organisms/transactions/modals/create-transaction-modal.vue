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
import {todayIsoDate} from "~/utils/date";

const props = withDefaults(defineProps<{
  title?: string;
  description?: string;
  filterType?: CategoryType;
}>(), {
  title: 'Create Transaction',
  description: 'Create a new transaction for your active account.'
});

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

const modeOptions = [
  {label: 'Standard', value: 'standard'},
  {label: 'Lending', value: 'lending'},
];

const form = ref<TransactionForm>(makeBlankTransactionForm());
const loanForm = ref<LoanFormModel>(makeBlankLoanForm());

const formRef = ref<InstanceType<typeof TransactionFormFields>>();
const loanFormRef = ref<InstanceType<typeof LoanFormFields>>();
const loading = ref(false);

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

watch(isOpen, (open) => {
  if (open) {
    mode.value = 'standard';
    form.value = makeBlankTransactionForm();
    loanForm.value = makeBlankLoanForm();
  }
});

async function handleSave() {
  if (mode.value === 'lending') {
    await saveLending();
  } else {
    await saveStandard();
  }
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
    toasts.success('Transaction created successfully.', 'Your transaction has been created.');
    emit('created');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, 'Transaction not created.', 'Your transaction could not be created, please try again.');
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
  } catch {
    // toast handled by store
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="description"
          :title="title">
    <template #body>
      <div class="space-y-4">
        <URadioGroup v-model="mode"
                     :items="modeOptions"
                     legend="Type"
                     orientation="horizontal"/>
        <TransactionFormFields v-if="mode === 'standard'"
                               ref="formRef"
                               v-model="form"
                               :filter-type="filterType"/>
        <LoanFormFields v-else
                        ref="loanFormRef"
                        v-model="loanForm"/>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave"> Create</UButton>
      </div>
    </template>
  </UModal>
</template>
