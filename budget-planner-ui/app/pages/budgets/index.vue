<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import BudgetsList from "~/components/_organisms/budgets/budgets-list.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

useHead({
  title: 'Budgets | Budget Planner',
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      description="Set monthly spending limits per category. Resets on the 1st of each month."
      title="Budgets"
    />
    <BudgetsList/>
  </UContainer>
</template>
