<script lang="ts" setup>
import CheckEmailCard from "~/components/_organisms/auth/check-email-card.vue";

definePageMeta({
  middleware: ['guest-guard'],
});

const {t} = useI18n();
useHead({
  title: t('auth.pageTitle.checkEmail'),
});
</script>

<template>
  <CheckEmailCard/>
</template>
