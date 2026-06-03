<script lang="ts" setup>
import Exports from "~/components/_organisms/exports/exports.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

const {t} = useI18n();

useHead({
  title: t('exports.page.title'),
});
</script>

<template>
  <Exports/>
</template>
