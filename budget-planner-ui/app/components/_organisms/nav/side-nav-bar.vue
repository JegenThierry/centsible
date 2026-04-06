<script setup lang="ts">
import { ref, computed } from 'vue';
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

const accountStore = useBudgetAccountsStore();
const open = ref(true)

const items = computed(() => {
  const accountId = accountStore.activeAccount?.id;
  const menuItems = [];

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

  menuItems.push({
    label: 'Accounts',
    to: '/accounts',
    icon: 'i-lucide-wallet'
  });

  return menuItems;
})
</script>

<template>
    <USidebar v-model:open="open"
              variant="sidebar"
              collapsible="icon"
              side="left"
              :ui="{ container: 'h-full' }">
      <template #header>
        <UIcon name="i-logos-nuxt-icon" class="size-8"/>
        Budget Planner
      </template>

      <UNavigationMenu
        :items="items"
        orientation="vertical"
        :ui="{ link: 'p-1.5 overflow-hidden' }"
      />
    </USidebar>
</template>

