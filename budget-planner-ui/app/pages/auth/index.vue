<script setup lang="ts">
import AuthForm from "~/components/auth/auth-form.vue";
import {useAuthStore} from "~/stores/authStore";

definePageMeta({
  middleware: [
    () => {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        return;
      }

      return navigateTo('/accounts');
    }
  ]
})

useHead({
  title: 'Welcome | Budget Planner',
});
</script>

<template>
  <AuthForm></AuthForm>
</template>
