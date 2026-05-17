<script lang="ts" setup>
import ConfirmationModal from "~/components/_organisms/modals/confirmation-modal.vue";
import type {Budget} from "~/models/budget/budget";
import {useBudgetService} from "~/services/budget/budget-service";

const props = defineProps<{
  budget: Budget | null;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'deleted'): void;
}>();

const service = useBudgetService(useApi());
const {t} = useI18n();

async function deleteBudget() {
  if (!props.budget) throw new Error("Missing budget");
  await service.remove(props.budget.id);
  emit('deleted');
}
</script>

<template>
  <ConfirmationModal v-model:open="isOpen"
                     :delete-callback="deleteBudget"
                     :entity="t('budgets.delete.entity')"/>
</template>
