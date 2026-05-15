<script lang="ts" setup>
import Dashboard from "~/components/_organisms/dashboard/dashboard.vue";

definePageMeta({
  middleware: ['auth-guard', 'account-loader']
})

useHead({
  title: 'Dashboard',
});
</script>

<template>
  <Dashboard/>
</template>
