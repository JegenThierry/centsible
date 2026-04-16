<script lang="ts" setup>
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
      icon: 'i-lucide-wallet',
      target: '_self'
    },
    {
      label: 'Categories',
      to: '/categories',
      icon: 'i-lucide-tag',
      target: '_self'
    },
    {
      label: 'Profile',
      to: '/profile',
      icon: 'i-lucide-user',
      target: '_self'
    }
  ];

  if (accountId) {
    menuItems.push(
      {
        label: 'Dashboard',
        to: `/${accountId}/dashboard`,
        icon: 'i-lucide-layout-dashboard',
        target: '_self'
      },
      {
        label: 'Transactions',
        to: `/${accountId}/transactions`,
        icon: 'i-lucide-arrow-right-left',
        target: '_self'
      }
    );
  }


  return menuItems;
})
</script>

<template>
  <USidebar
    v-model:open="open"
    :ui="{
      container: 'h-full m-0',
      content: 'rounded-none sm:rounded-xl'
    }"
    collapsible="icon"
    side="left"
    variant="inset"
  >
    <template #header>
      <div class="flex items-center justify-between w-full">
        <UIcon class="size-8" name="i-logos-nuxt-icon"/>
        <UButton
          aria-label="Close sidebar"
          class="lg:hidden"
          color="neutral"
          icon="i-lucide-x"
          variant="ghost"
          @click="open = false"
        />
      </div>
    </template>

    <UNavigationMenu
      :items="items"
      :ui="{ link: 'p-1.5 overflow-hidden' }"
      orientation="vertical"
    />
  </USidebar>
</template>

