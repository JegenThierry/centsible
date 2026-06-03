<script lang="ts" setup>
import Budgets from "~/components/_organisms/budgets/budgets.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

const {t} = useI18n();

useHead({
  title: t('budgets.page.title'),
});
</script>

<template>
  <Budgets/>
</template>
