<script lang="ts" setup>
import RefreshButton from "~/components/_molecules/buttons/refresh-button.vue";
import CreateBudgetAccountButton from "~/components/_organisms/buttons/create-budget-account-button.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";

const emit = defineEmits([
  'refresh-accounts',
])

const {t} = useI18n();

function onRefreshAccounts() {
  emit('refresh-accounts');
}
</script>

<template>
  <AppEmptyState
    class="max-w-2xl"
    :description="t('accounts.list.emptyDescription')"
    icon="i-lucide-bar-chart-big"
    :title="t('accounts.list.emptyTitle')"
  >
    <template #actions>
      <RefreshButton @refresh="onRefreshAccounts"/>
      <CreateBudgetAccountButton/>
    </template>
  </AppEmptyState>
</template>
