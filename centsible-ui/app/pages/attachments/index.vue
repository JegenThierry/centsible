<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import AllAttachmentsList from "~/components/_organisms/attachments/all-attachments-list.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

const {t} = useI18n();

useHead({
  title: t('attachments.pageTitle'),
});
</script>

<template>
  <UContainer class="py-6 sm:py-10 space-y-4 sm:space-y-6">
    <PageHeader :title="t('attachments.title')" :description="t('attachments.description')"/>
    <AllAttachmentsList/>
  </UContainer>
</template>
