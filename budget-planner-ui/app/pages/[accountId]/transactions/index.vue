<script setup lang="ts">
import TransactionList from "~/components/transactions/transaction-list.vue";

definePageMeta({
  middleware: ['auth-guard', 'account-loader']
})

useHead({
  title: 'Transactions | Budget Planner',
});
</script>

<template>
  <TransactionList />
</template>
