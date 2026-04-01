<script setup lang="ts">

import Profile from "~/components/_organisms/profile.vue";
import type {UserDto} from "~/models/user/user-dto";
import {useUserService} from "~/services/user/user-service";

const api = useApi();
const user = ref<UserDto>();
const accountStore = useBudgetAccountsStore();

onMounted(async () => {
  user.value = await useUserService(api).fetchMyself();
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
      <Profile v-if="user" :user="user" />
    </template>
  </UHeader>
</template>

<style scoped>

</style>
