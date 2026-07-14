<script lang="ts" setup>
import ConfirmationModal from "~/components/_organisms/modals/confirmation-modal.vue";
import type {Transaction} from "~/models/transactions/transaction";

const props = defineProps<{
  transaction: Transaction | null;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'confirm'): void;
}>();

const {t} = useI18n();

const isTransfer = computed(() => !!props.transaction?.transferGroupId);

const entity = computed(() =>
  isTransfer.value ? t('transactions.transfer.entity') : t('transactions.delete.entity'),
);

// Regular deletes get an undo window, so promise it in the copy; transfers are truly irreversible.
const body = computed(() =>
  isTransfer.value ? undefined : t('transactions.delete.confirmUndoable'),
);

/** Pure confirmation gate — the transaction list owns the actual (deferred, undoable) delete. */
async function onConfirm() {
  emit('confirm');
}
</script>

<template>
  <ConfirmationModal v-model:open="isOpen"
                     :delete-callback="onConfirm"
                     :entity="entity"
                     :body="body"
                     :manage-toasts="false"/>
</template>
