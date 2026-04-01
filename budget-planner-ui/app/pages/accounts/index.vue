<script setup lang="ts">
import Accounts from "~/components/accounts/accounts.vue";

definePageMeta({
  middleware: ['auth-guard']
})

useHead({
  title: 'Accounts | Budget Planner',
});
</script>

<template>
  <Accounts />
</template>
