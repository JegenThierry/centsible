<script lang="ts" setup>
import Integrations from "~/components/_organisms/integrations/integrations.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

const {t} = useI18n();

useHead({
  title: t('integrations.page.title'),
});
</script>

<template>
  <Integrations/>
</template>
