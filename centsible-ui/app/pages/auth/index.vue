<script lang="ts" setup>
import AuthForm from "~/components/_organisms/auth/auth-form.vue";

definePageMeta({
  middleware: [
    'guest-guard',
    // Fresh instance with no users yet → send the operator to first-run setup.
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
