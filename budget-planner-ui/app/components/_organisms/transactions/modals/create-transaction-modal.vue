<script setup lang="ts">
import {type TransactionForm} from "~/models/transactions/transaction";
import { CategoryType } from "~/models/category/category";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import TransactionFormFields from "~/components/_molecules/transactions/transaction-form.vue";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {useToasts} from "~/services/toasts/toast-service";
import {format} from 'date-fns';

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
const toasts = useToasts();
const budgetAccountsStore = useBudgetAccountsStore();

const form = ref<TransactionForm>({
  amount: 0,
  description: '',
  category: undefined,
  transactionDate: format(new Date(), 'yyyy-MM-dd'),
});

const formRef = ref<InstanceType<typeof TransactionFormFields>>();
const loading = ref(false);

function resetForm() {
  form.value = {
    amount: 0,
    description: '',
    category: undefined,
    transactionDate: format(new Date(), 'yyyy-MM-dd'),
  };
}

onMounted(() => {
  resetForm();
});

async function handleSave() {
  if (!formRef.value?.validate()) {
    return;
  }

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
    toasts.error('Transaction not created.', 'Your transaction could not be created, please try again.');
    console.error('Failed to save transaction:', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :title="title"
          :description="description">
    <template #body>
      <TransactionFormFields ref="formRef" v-model="form" :filter-type="filterType" />
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave"> Create</UButton>
      </div>
    </template>
  </UModal>
</template>
