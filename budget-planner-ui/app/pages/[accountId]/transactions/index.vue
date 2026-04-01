<script setup lang="ts">
import TransactionList from "~/components/transactions/transaction-list.vue";

definePageMeta({
  middleware: ['account-loader']
})

useHead({
  title: 'Transactions | Budget Planner',
});
</script>

<template>
  <TransactionList />
</template>
