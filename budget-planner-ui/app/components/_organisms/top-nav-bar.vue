<script setup lang="ts">

import Profile from "~/components/_organisms/profile.vue";
import type {UserDto} from "~/models/user/user-dto";
import {useUserService} from "~/services/user/user-service";

const api = useApi();
const user = ref<UserDto>();

onMounted(async () => {
  user.value = await useUserService(api).fetchMyself();
})
</script>

<template>
  <UHeader title="Budget Planner">
    <template #right>
      <UColorModeButton />
      <Profile v-if="user" :user="user" />
    </template>
  </UHeader>
</template>

<style scoped>

</style>