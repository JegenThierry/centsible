<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import RecurringList from "~/components/_organisms/recurring/recurring-list.vue";

definePageMeta({
  middleware: ['auth-guard', 'account-loader'],
});

useHead({
  title: 'Recurring',
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      description="Schedule transactions that repeat automatically — rent, salary, subscriptions."
      title="Recurring transactions"
    />
    <RecurringList/>
  </UContainer>
</template>
