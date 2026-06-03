<script lang="ts" setup>
import ForgotPasswordCard from "~/components/_organisms/auth/forgot-password-card.vue";

definePageMeta({
  middleware: [
    () => {
      const authStore = useAuthStore();
      if (authStore.isAuthenticated) {
        return navigateTo('/accounts');
      }
    },
  ],
});

const {t} = useI18n();
useHead({
  title: t('auth.forgotPassword.pageTitle'),
});
</script>

<template>
  <ForgotPasswordCard/>
</template>
