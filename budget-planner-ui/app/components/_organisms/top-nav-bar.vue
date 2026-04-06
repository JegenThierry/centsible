<script setup lang="ts">

import Profile from "~/components/_organisms/profile.vue";
import {useUserStore} from "~/stores/userStore";
import {useAuthStore} from "~/stores/authStore";

const userStore = useUserStore();
const authStore = useAuthStore();

onMounted(async () => {
  if (authStore.isAuthenticated) {
    await userStore.fetchMyself();
  }
})

</script>

<template>
  <UHeader title="Budget Planner">

    <template #right>
      <UColorModeButton />
      <Profile v-if="userStore.user" :user="userStore.user" />
    </template>
  </UHeader>
</template>

<style scoped>

</style>
