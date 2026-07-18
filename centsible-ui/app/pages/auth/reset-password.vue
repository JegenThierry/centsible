<script lang="ts" setup>
import ResetPasswordCard from "~/components/_organisms/auth/reset-password-card.vue";

definePageMeta({
  middleware: ['guest-guard'],
});

const {t} = useI18n();
useHead({
  title: t('auth.resetPassword.pageTitle'),
});
</script>

<template>
  <ResetPasswordCard/>
</template>
