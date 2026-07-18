<script lang="ts" setup>
import TagsManager from "~/components/_organisms/tags/tags-manager.vue";

definePageMeta({
  middleware: ['auth-guard']
})

const {t} = useI18n();

useHead({
  title: t('tags.page.title'),
});
</script>

<template>
  <TagsManager/>
</template>
