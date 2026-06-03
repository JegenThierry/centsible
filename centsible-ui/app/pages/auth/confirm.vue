<script lang="ts" setup>
import ConfirmCard from "~/components/_organisms/auth/confirm-card.vue";

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
  title: t('auth.pageTitle.confirm'),
});
</script>

<template>
  <ConfirmCard/>
</template>
