<script setup lang="ts">
import type {UserDto} from "~/models/user/user-dto";

const props = defineProps<{
  user: UserDto,
}>()

</script>

<template>
  <UUser
      :name="user.name"
      :description="user.email"
      :avatar="{
        src: 'https://i.pravatar.cc/150?u=john-doe'
      }"
  />
</template>

<style scoped>

</style>