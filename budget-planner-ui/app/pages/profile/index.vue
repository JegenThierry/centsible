<script setup lang="ts">
import Profile from "~/components/_organisms/user/profile.vue";

definePageMeta({
  middleware: ['auth-guard']
})

useHead({
  title: 'Profile | Budget Planner',
});
</script>

<template>
  <Profile />
</template>
