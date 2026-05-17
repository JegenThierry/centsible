<script lang="ts" setup>
import Accounts from "~/components/_organisms/accounts/accounts.vue";

definePageMeta({
  middleware: ['auth-guard', 'onboarding-guard']
})

const {t} = useI18n();

useHead({
  title: () => t('accounts.pageTitle'),
});
</script>

<template>
  <Accounts/>
</template>
