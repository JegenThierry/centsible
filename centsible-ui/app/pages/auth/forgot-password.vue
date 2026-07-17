<script lang="ts" setup>
import ForgotPasswordCard from "~/components/_organisms/auth/forgot-password-card.vue";

definePageMeta({
  middleware: ['guest-guard'],
});

const {t} = useI18n();
useHead({
  title: t('auth.forgotPassword.pageTitle'),
});
</script>

<template>
  <ForgotPasswordCard/>
</template>
