<script lang="ts" setup>
import Recurring from "~/components/_organisms/recurring/recurring.vue";

definePageMeta({
  middleware: ['auth-guard', 'account-loader'],
});

const {t} = useI18n();

useHead({
  title: () => t('transactions.recurring.pageTitle'),
});
</script>

<template>
  <Recurring/>
</template>
