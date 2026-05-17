<script lang="ts" setup>
import {type Transaction, type TransactionForm} from "~/models/transactions/transaction";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import TransactionFormFields from "~/components/_molecules/transactions/transaction-form.vue";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useModalDirtyGuard} from "~/composables/use-unsaved-changes-guard";
import {todayIsoDate} from "~/utils/date";

const props = defineProps<{
  transaction: Transaction;
}>();
const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'updated'): void;
}>();

const api = useApi();
const transactionService = useTransactionService(api);
const toasts = useToasts();
const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const form = ref<TransactionForm>({
  amount: 0,
  description: '',
  category: undefined,
  transactionDate: todayIsoDate(),
});

const formRef = ref<InstanceType<typeof TransactionFormFields>>();
const loading = ref(false);
const formId = useId();
const activeCurrency = computed(() => budgetAccountsStore.activeAccount?.currency);

function loadTransaction(transaction: Transaction) {
  form.value = {
    amount: transaction.amount,
    description: transaction.description,
    category: transaction.category,
    transactionDate: transaction.transactionDate.split('T')[0],
  };
}

const {requestClose} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => form.value,
  onResetOnOpen: () => loadTransaction(props.transaction),
});

async function handleEdit() {
  if (loading.value) return;
  if (!formRef.value?.validate()) return;
  if (!budgetAccountsStore.activeAccount?.id) return;
  if (props.transaction.id === undefined) return;
  if (!form.value.category?.id || !form.value.transactionDate) return;

  loading.value = true;
  try {
    await transactionService.updateTransaction(
      budgetAccountsStore.activeAccount.id,
      props.transaction.id,
      {
        amount: form.value.amount,
        description: form.value.description,
        categoryId: form.value.category.id,
        transactionDate: form.value.transactionDate
      }
    );
    emit('updated');
    toasts.success(t('transactions.edit.toastSuccessTitle'), t('transactions.edit.toastSuccessBody'));
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, t('transactions.edit.toastErrorTitle'), t('transactions.edit.toastErrorBody'));
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
      <UForm :id="formId" :state="form" @submit="handleEdit">
        <TransactionFormFields ref="formRef"
                               v-model="form"
                               :currency="activeCurrency"
                               :disabled="loading"/>
      </UForm>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton :disabled="loading" @click="requestClose(false)"/>
        <UButton :form="formId" :loading="loading" type="submit">{{ t('transactions.edit.submit') }}</UButton>
      </div>
    </template>
  </UModal>
</template>
