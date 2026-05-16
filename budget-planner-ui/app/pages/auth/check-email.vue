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

const route = useRoute();
const email = computed(() => {
  const value = route.query.email;
  return typeof value === 'string' ? value : undefined;
});
</script>

<template>
  <CheckEmailCard :email="email"/>
</template>
