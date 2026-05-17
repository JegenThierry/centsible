<script lang="ts" setup>
import Landing from "~/components/_organisms/landing/landing.vue";
import {useAuthStore} from "~/stores/authStore";
import {useAuthService} from "~/services/auth/auth-service";

definePageMeta({
  middleware: [
    async () => {
      const nuxtApp = useNuxtApp();
      const authStore = useAuthStore();
      const api = useApi();
      const authService = useAuthService(api);

      if (import.meta.client && authStore.isAuthenticated) {
        return navigateTo('/accounts');
      }

      try {
        const isVerified = await authService.verify();
        if (isVerified) {
          authStore.setAuthenticated(true);
          return await nuxtApp.runWithContext(() => navigateTo('/accounts'));
        }
      } catch (error) {
        console.warn('Session verification failed; staying on landing', error);
      }
    }
  ]
});

useHead({
  title: 'Centsible',
});
</script>

<template>
  <Landing/>
</template>
