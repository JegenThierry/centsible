<script lang="ts" setup>
import Categories from "~/components/_organisms/categories/categories.vue";

definePageMeta({
  middleware: ['auth-guard']
})

useHead({
  title: 'Categories',
});
</script>

<template>
  <Categories/>
</template>
