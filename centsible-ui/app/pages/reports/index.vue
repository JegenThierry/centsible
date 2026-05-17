<script lang="ts" setup>
import Reports from "~/components/_organisms/reports/reports.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

const {t} = useI18n();

useHead({
  title: t('reports.page.title'),
});
</script>

<template>
  <Reports/>
</template>
