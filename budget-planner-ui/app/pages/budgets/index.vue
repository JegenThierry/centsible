<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import BudgetsList from "~/components/_organisms/budgets/budgets-list.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

const {t} = useI18n();

useHead({
  title: t('budgets.page.title'),
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      :description="t('budgets.page.description')"
      :title="t('budgets.page.title')"
    />
    <BudgetsList/>
  </UContainer>
</template>
