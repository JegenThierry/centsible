<script lang="ts" setup>
import TotpChallengeCard from "~/components/_organisms/auth/totp-challenge-card.vue";
import {useAuthStore} from "~/stores/authStore";

definePageMeta({
  middleware: [
    () => {
      const authStore = useAuthStore();
      if (authStore.isAuthenticated) {
        return navigateTo('/accounts');
      }
      if (!authStore.twoFactorPending) {
        return navigateTo('/auth');
      }
    },
  ],
});

const {t} = useI18n();
useHead({
  title: t('auth.twoFactor.challenge.pageTitle'),
});
</script>

<template>
  <TotpChallengeCard/>
</template>
