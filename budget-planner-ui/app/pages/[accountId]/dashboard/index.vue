<script lang="ts" setup>
import Dashboard from "~/components/_organisms/dashboard/dashboard.vue";

definePageMeta({
  middleware: ['auth-guard', 'account-loader']
})

const {t} = useI18n();

useHead({
  title: () => t('accounts.dashboard.pageTitle'),
});
</script>

<template>
  <Dashboard/>
</template>
