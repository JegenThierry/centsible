<script lang="ts" setup>
import Admin from "~/components/_organisms/admin/admin.vue";

definePageMeta({
  middleware: ['auth-guard', 'admin-guard'],
});

const {t} = useI18n();

useHead({
  title: t('admin.page.title'),
});
</script>

<template>
  <Admin/>
</template>
