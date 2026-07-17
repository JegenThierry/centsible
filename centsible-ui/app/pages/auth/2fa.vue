<script lang="ts" setup>
import TotpChallengeCard from "~/components/_organisms/auth/totp-challenge-card.vue";

definePageMeta({
  middleware: [
    'guest-guard',
    // A challenge is only reachable mid-login, once the password step has flagged 2FA as pending.
    () => {
      const authStore = useAuthStore();
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
