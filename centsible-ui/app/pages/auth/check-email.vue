<script lang="ts" setup>
import CheckEmailCard from "~/components/_organisms/auth/check-email-card.vue";

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
  title: t('auth.pageTitle.checkEmail'),
});
</script>

<template>
  <CheckEmailCard/>
</template>
