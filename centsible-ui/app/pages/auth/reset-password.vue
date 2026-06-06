<script lang="ts" setup>
import ResetPasswordCard from "~/components/_organisms/auth/reset-password-card.vue";

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
  title: t('auth.resetPassword.pageTitle'),
});
</script>

<template>
  <ResetPasswordCard/>
</template>
