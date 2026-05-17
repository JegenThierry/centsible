<script lang="ts" setup>
import Landing from "~/components/_organisms/landing/landing.vue";
import {useAuthStore} from "~/stores/authStore";
import {useAuthService} from "~/services/auth/auth-service";

definePageMeta({
  middleware: [
    async () => {
      const authStore = useAuthStore();
      const api = useApi();
      const authService = useAuthService(api);

      // In-app navigation: trust the flag and skip the roundtrip.
      if (import.meta.client && authStore.isAuthenticated) {
        return navigateTo('/accounts');
      }

      // Cold load: verify the cookie so authenticated users skip the landing.
      try {
        const isVerified = await authService.verify();
        if (isVerified) {
          authStore.setAuthenticated(true);
          return navigateTo('/accounts');
        }
      } catch {
        // Not authenticated or transient failure — fall through to the landing page.
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
