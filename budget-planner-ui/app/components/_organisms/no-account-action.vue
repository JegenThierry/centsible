<script setup lang="ts">
import RefreshButton from "~/components/_molecules/buttons/refresh-button.vue";
import CreateBudgetAccountButton from "~/components/_organisms/buttons/create-budget-account-button.vue";

const emit = defineEmits([
  'refresh-accounts',
])

function onRefreshAccounts() {
  emit('refresh-accounts');
}
</script>

<template>
  <UEmpty class="max-w-2xl"
          icon="i-lucide-bar-chart-big"
          title="No accounts found"
          description="It looks like you haven't created any account yet. Create one to get started."
  >
    <template #actions>
      <RefreshButton @refresh="onRefreshAccounts"/>
      <CreateBudgetAccountButton />
    </template>
  </UEmpty>
</template>
