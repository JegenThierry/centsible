<script setup lang="ts">
import {type Category} from "~/models/category/category";
import {
  type Transaction,
  type TransactionForm,
  TransactionType,
  transactionTypes
} from "~/models/transactions/transaction";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {useCategoryService} from "~/services/category/category-service";
import {useValidator} from "~/composables/use-validator";
import {useToasts} from "~/services/toasts/toast-service";

const props = defineProps<{
  transaction: Transaction;
}>();
const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'edited'): void;
}>();

const api = useApi();
const transactionService = useTransactionService(api);
const categoryService = useCategoryService(api);
const toasts = useToasts();
const budgetAccountsStore = useBudgetAccountsStore();

const form = ref<TransactionForm>({
  amount: 0,
  type: TransactionType.EXPENSE,
  description: '',
  category: undefined as Category | undefined,
  transactionDate: new Date().toISOString().split('T')[0],
});

const categories = ref<Category[]>([]);

const amountInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const categoryInput = ref();
const dateInput = ref();

const loading = ref(false);

async function loadCategories() {
  try {
    categories.value = await categoryService.fetchCategories();
  } catch (error) {
    console.error('Failed to load categories:', error);
  }
}

function loadTransaction(transaction: Transaction) {
  form.value = {
    amount: transaction.amount,
    type: transaction.type,
    description: transaction.description,
    category: transaction.category,
    transactionDate: props.transaction.transactionDate.split('T')[0],
  };
}

async function handleEdit() {
  const inputs = [amountInput, descriptionInput, categoryInput, dateInput];
  if (!useValidator().validateInputs(inputs)) {
    return;
  }

  if (!budgetAccountsStore.activeAccount?.id || props.transaction.id == undefined) return;

  loading.value = true;
  try {
    await transactionService.updateTransaction(
      budgetAccountsStore.activeAccount.id,
      props.transaction.id,
      {
        amount: form.value.amount,
        type: form.value.type,
        description: form.value.description,
        categoryId: form.value.category?.id,
        transactionDate: form.value.transactionDate
      }
    );
    emit('edited');
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
  loadCategories();
  loadTransaction(props.transaction)
});
</script>

<template>
  <UModal v-model:open="isOpen"
          title="Edit Transaction"
          description="Edit your transaction for your active account.">
    <template #body>
      <div class="space-y-4">
        <URadioGroup v-model="form.type"
                     :items="transactionTypes"
                     legend="Transaction Type"
                     orientation="horizontal"/>

        <BaseInput ref="amountInput"
                   v-model="form.amount"
                   label="Amount"
                   type="number"
                   required
                   placeholder="0.00"/>

        <CategorySelect ref="categoryInput"
                        v-model="form.category"
                        label="Category"
                        :options="categories"
                        required/>

        <BaseInput ref="descriptionInput"
                   v-model="form.description"
                   label="Description"
                   type="text"
                   required
                   placeholder="Lunch, Groceries, etc."/>

        <DateInput ref="dateInput"
                   v-model="form.transactionDate"
                   label="Date"
                   required/>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <UButton color="neutral" variant="ghost" @click="isOpen = false">Cancel</UButton>
        <UButton :loading="loading" @click="handleEdit">Edit</UButton>
      </div>
    </template>
  </UModal>
</template>
