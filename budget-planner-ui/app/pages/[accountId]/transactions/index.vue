<script setup lang="ts">
import TransactionList from "~/components/_organisms/transactions/transaction-list.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";

definePageMeta({
  middleware: ['auth-guard', 'account-loader']
})

useHead({
  title: 'Transactions | Budget Planner',
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      title="Transactions"
      description="View and manage your transactions"
    />
    <TransactionList />
  </UContainer>
</template>
