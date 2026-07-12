<script lang="ts" setup>
import ConfirmationModal from "~/components/_organisms/modals/confirmation-modal.vue";
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

const props = defineProps<{
  account: BudgetAccount | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'deleted'): void;
}>();

const accountStore = useBudgetAccountsStore();
const {t} = useI18n();

async function deleteAccount() {
  if (!props.account?.id) return;
  await accountStore.deleteAccount(props.account.id);
  emit('deleted');
}
</script>

<template>
  <ConfirmationModal v-model:open="isOpen"
                     :entity="account?.name ?? ''"
                     :title="t('accounts.modals.delete.title')"
                     :body="t('accounts.modals.delete.body', {name: account?.name ?? ''})"
                     :confirm-label="t('accounts.modals.delete.submit')"
                     :delete-callback="deleteAccount"/>
</template>
