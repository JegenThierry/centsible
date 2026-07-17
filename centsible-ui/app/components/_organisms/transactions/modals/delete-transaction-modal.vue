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

const body = computed(() =>
  isTransfer.value ? undefined : t('transactions.delete.confirmUndoable'),
);

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
