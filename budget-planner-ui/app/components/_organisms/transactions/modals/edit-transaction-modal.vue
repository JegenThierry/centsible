<script lang="ts" setup>
import {type Transaction, type TransactionForm} from "~/models/transactions/transaction";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import TransactionFormFields from "~/components/_molecules/transactions/transaction-form.vue";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {useToasts} from "~/services/toasts/toast-service";

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

const form = ref<TransactionForm>({
  amount: 0,
  description: '',
  category: undefined,
  transactionDate: new Date().toISOString().split('T')[0],
});

const formRef = ref<InstanceType<typeof TransactionFormFields>>();
const loading = ref(false);

function loadTransaction(transaction: Transaction) {
  form.value = {
    amount: transaction.amount,
    description: transaction.description,
    category: transaction.category,
    transactionDate: props.transaction.transactionDate.split('T')[0],
  };
}

async function handleEdit() {
  if (!formRef.value?.validate()) {
    return;
  }

  if (!budgetAccountsStore.activeAccount?.id || props.transaction.id == undefined) return;
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
    toasts.success('Transaction updated successfully.', 'Your transaction has been updated.');
    isOpen.value = false;
  } catch (error) {
    toasts.error('Transaction not updated.', 'Your transaction could not be updated, please try again.');
    console.error('Failed to save transaction:', error);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadTransaction(props.transaction)
});
</script>

<template>
  <UModal v-model:open="isOpen"
          description="Edit your transaction for your active account."
          title="Edit Transaction">
    <template #body>
      <TransactionFormFields ref="formRef" v-model="form"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleEdit">Edit</UButton>
      </div>
    </template>
  </UModal>
</template>
