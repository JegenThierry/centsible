<script lang="ts" setup>
import SetupWizard from "~/components/_organisms/auth/setup-wizard.vue";

definePageMeta({
  middleware: 'setup-guard',
});

const {t} = useI18n();
useHead({
  title: t('auth.setup.pageTitle'),
});
</script>

<template>
  <SetupWizard/>
</template>
