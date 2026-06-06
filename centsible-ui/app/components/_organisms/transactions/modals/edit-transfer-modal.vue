<script lang="ts" setup>
import type {Transaction, TransferForm} from "~/models/transactions/transaction";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import TransferFormFields from "~/components/_molecules/transactions/transfer-form.vue";
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

const form = ref<TransferForm>(makeBlankForm());
const transferFormRef = ref<InstanceType<typeof TransferFormFields>>();
const loading = ref(false);
const formId = useId();

function makeBlankForm(): TransferForm {
  return {
    amount: 0,
    sourceAccountId: undefined,
    destinationAccountId: undefined,
    description: '',
    transactionDate: todayIsoDate(),
  };
}

async function loadTransfer() {
  try {
    const details = await transactionService.fetchTransfer(props.transaction.id);
    form.value = {
      amount: details.amount,
      sourceAccountId: details.sourceAccountId,
      destinationAccountId: details.destinationAccountId,
      description: details.description ?? '',
      transactionDate: details.transactionDate.split('T')[0]!,
    };
    nextTick(captureSnapshot);
  } catch (error) {
    useApiErrors().toastError(error, t('transactions.transfer.toastErrorTitle'), t('transactions.transfer.loadErrorBody'));
    isOpen.value = false;
  }
}

const {requestClose, captureSnapshot} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => form.value,
  onResetOnOpen: () => {
    form.value = makeBlankForm();
    if (budgetAccountsStore.availableAccounts.length === 0) budgetAccountsStore.updateAvailableAccounts();
    loadTransfer();
  },
});

async function handleSave() {
  if (loading.value) return;
  if (!transferFormRef.value?.validate()) return;
  const sourceId = form.value.sourceAccountId;
  const destinationId = form.value.destinationAccountId;
  if (!sourceId || !destinationId || !form.value.transactionDate) return;
  if (sourceId === destinationId) {
    toasts.error(t('transactions.transfer.sameAccountTitle'), t('transactions.transfer.sameAccountBody'));
    return;
  }

  loading.value = true;
  try {
    await transactionService.updateTransfer(sourceId, props.transaction.id, {
      amount: form.value.amount,
      destinationAccountId: destinationId,
      description: form.value.description,
      transactionDate: form.value.transactionDate,
    });
    toasts.success(t('transactions.transfer.editToastSuccessTitle'), t('transactions.transfer.editToastSuccessBody'));
    emit('updated');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, t('transactions.transfer.toastErrorTitle'), t('transactions.transfer.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal :open="isOpen"
          :description="t('transactions.transfer.editDescription')"
          :title="t('transactions.transfer.editTitle')"
          @update:open="requestClose">
    <template #body>
      <UForm :id="formId" :state="form" @submit="handleSave">
        <TransferFormFields ref="transferFormRef"
                            v-model="form"
                            :accounts="budgetAccountsStore.availableAccounts"
                            :disabled="loading"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="t('transactions.edit.submit')"
                          @cancel="requestClose(false)"/>
    </template>
  </UModal>
</template>
