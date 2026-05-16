<script lang="ts" setup>
import Profile from "~/components/_organisms/user/profile.vue";

definePageMeta({
  middleware: ['auth-guard']
})

const {t} = useI18n();

useHead({
  title: t('profile.page.title'),
});
</script>

<template>
  <Profile/>
</template>
