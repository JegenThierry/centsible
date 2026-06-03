<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import RecurringList from "~/components/_organisms/recurring/recurring-list.vue";

const {t} = useI18n();
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      :description="t('transactions.recurring.description')"
      :title="t('transactions.recurring.title')"
    />
    <RecurringList/>
  </UContainer>
</template>
