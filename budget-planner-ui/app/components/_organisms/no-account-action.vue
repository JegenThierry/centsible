<script setup lang="ts">
import type {ButtonProps} from "@nuxt/ui/components/Button.vue";

const emit = defineEmits([
  'refresh-accounts',
  'create-budget-account',
])

function onCreateAccount() {
  emit('create-budget-account');
}

function onRefreshAccounts() {
  emit('refresh-accounts');
}

const actions: ButtonProps[] = [
  {
    icon: 'i-lucide-refresh-cw',
    label: 'Refresh',
    color: 'neutral',
    variant: 'subtle',
    onClick: onRefreshAccounts,
  },
  {
    icon: 'i-lucide-plus',
    label: 'Create Budget Account',
    onClick: onCreateAccount,
  }
]
</script>

<template>
  <UEmpty class="max-w-2xl"
          icon="i-lucide-bar-chart-big"
          title="No accounts found"
          description="It looks like you haven't created any account yet. Create one to get started."
          :actions="actions" />
</template>

<style scoped>

</style>
