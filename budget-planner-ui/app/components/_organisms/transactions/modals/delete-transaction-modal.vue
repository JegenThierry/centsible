<script lang="ts" setup>
import ConfirmationModal from "~/components/_organisms/modals/confirmation-modal.vue";
import type {Transaction} from "~/models/transactions/transaction";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useApi} from "~/composables/use-api";

const props = defineProps<{
  transaction: Transaction | null;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'deleted'): void;
}>();

const api = useApi();
const transactionService = useTransactionService(api);
const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();

async function deleteTransaction() {
  if (!props.transaction || !budgetAccountsStore.activeAccount?.id) {
    throw new Error("Missing transaction or active account");
  }

  await transactionService.deleteTransaction(
    budgetAccountsStore.activeAccount.id,
    props.transaction.id
  );
  emit('deleted');
}
</script>

<template>
  <ConfirmationModal v-model:open="isOpen"
                     :delete-callback="deleteTransaction"
                     :entity="t('transactions.delete.entity')"/>
</template>
