<script lang="ts" setup>
import Loans from "~/components/_organisms/loans/loans.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

const {t} = useI18n();

useHead({
  title: t('contacts.loansPage.title'),
});
</script>

<template>
  <Loans/>
</template>
