<script lang="ts" setup>
import Transactions from "~/components/_organisms/transactions/transactions.vue";

definePageMeta({
  middleware: ['auth-guard', 'account-loader']
})

const {t} = useI18n();

useHead({
  title: () => t('transactions.pageTitle'),
});
</script>

<template>
  <Transactions/>
</template>
