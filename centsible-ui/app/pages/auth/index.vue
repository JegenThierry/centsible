<script lang="ts" setup>
import AuthForm from "~/components/_organisms/auth/auth-form.vue";
import {useAuthStore} from "~/stores/authStore";

definePageMeta({
  middleware: [
    () => {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        return;
      }

      return navigateTo('/accounts');
    },
    'setup-guard',
  ]
})

const {t} = useI18n();
useHead({
  title: t('auth.pageTitle.welcome'),
});
</script>

<template>
  <AuthForm></AuthForm>
</template>
