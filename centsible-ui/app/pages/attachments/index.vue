<script lang="ts" setup>
import Attachments from "~/components/_organisms/attachments/attachments.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

const {t} = useI18n();

useHead({
  title: t('attachments.pageTitle'),
});
</script>

<template>
  <Attachments/>
</template>
