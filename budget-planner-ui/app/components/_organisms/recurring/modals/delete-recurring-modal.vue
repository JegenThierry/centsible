<script lang="ts" setup>
import ConfirmationModal from "~/components/_organisms/modals/confirmation-modal.vue";
import type {RecurringTransaction} from "~/models/recurring/recurring-transaction";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";

const props = defineProps<{
  rule: RecurringTransaction | null;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'deleted'): void;
}>();

const api = useApi();
const service = useRecurringTransactionService(api);
const {t} = useI18n();

async function deleteRule() {
  if (!props.rule) throw new Error("Missing rule");
  await service.remove(props.rule.id);
  emit('deleted');
}
</script>

<template>
  <ConfirmationModal v-model:open="isOpen"
                     :delete-callback="deleteRule"
                     :entity="t('transactions.recurring.delete.entity')"/>
</template>
