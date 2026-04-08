<script setup lang="ts">
import {computed} from 'vue';
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useSidebar} from "~/composables/use-sidebar";

const accountStore = useBudgetAccountsStore();
const {open} = useSidebar()

const items = computed(() => {
  const accountId = accountStore.activeAccount?.id;
  const menuItems = [
    {
      label: 'Accounts',
      to: '/accounts',
      icon: 'i-lucide-wallet'
    }
  ];

  if (accountId) {
    menuItems.push(
      {
        label: 'Dashboard',
        to: `/${accountId}/dashboard`,
        icon: 'i-lucide-layout-dashboard'
      },
      {
        label: 'Transactions',
        to: `/${accountId}/transactions`,
        icon: 'i-lucide-arrow-right-left'
      }
    );
  }


  return menuItems;
})
</script>

<template>
  <USidebar
    v-model:open="open"
    variant="inset"
    collapsible="icon"
    side="left"
    :ui="{ container: 'h-full' }"
  >
    <template #header>
      <UIcon name="i-logos-nuxt-icon" class="size-8" />
    </template>

    <UNavigationMenu
      :items="items"
      orientation="vertical"
      :ui="{ link: 'p-1.5 overflow-hidden' }"
    />
  </USidebar>
</template>

