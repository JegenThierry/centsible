<script setup lang="ts">
import Dashboard from "~/components/dashboard/dashboard.vue";

definePageMeta({
  middleware: ['account-loader']
})

useHead({
  title: 'Dashboard | Budget Planner',
});
</script>

<template>
  <Dashboard/>
</template>
