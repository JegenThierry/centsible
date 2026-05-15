<script lang="ts" setup>
import Profile from "~/components/_organisms/user/profile.vue";

definePageMeta({
  middleware: ['auth-guard']
})

useHead({
  title: 'Profile',
});
</script>

<template>
  <Profile/>
</template>
