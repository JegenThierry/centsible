<script setup lang="ts">

import Profile from "~/components/_organisms/profile.vue";
import {useUserStore} from "~/stores/userStore";

const accountStore = useBudgetAccountsStore();
const userStore = useUserStore();

onMounted(async () => {
  await userStore.fetchMyself();
})

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
  <UHeader title="Budget Planner">
    <UNavigationMenu :items="items" class="justify-center" />

    <template #right>
      <UColorModeButton />
      <Profile v-if="userStore.user" :user="userStore.user" />
    </template>
  </UHeader>
</template>

<style scoped>

</style>
