<script lang="ts" setup>
import OnboardingWizard from "~/components/_organisms/onboarding/onboarding-wizard.vue";

definePageMeta({middleware: ['auth-guard']});

const {t} = useI18n();

useHead({title: () => t('onboarding.pageTitle')});
</script>

<template>
  <OnboardingWizard/>
</template>
