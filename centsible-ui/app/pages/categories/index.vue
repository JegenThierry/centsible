<script lang="ts" setup>
import Categories from "~/components/_organisms/categories/categories.vue";

definePageMeta({
  middleware: ['auth-guard']
})

const {t} = useI18n();

useHead({
  title: t('categories.page.title'),
});
</script>

<template>
  <Categories/>
</template>
