<script lang="ts" setup>
import Categories from "~/components/_organisms/categories/categories.vue";

definePageMeta({
  middleware: ['auth-guard']
})

useHead({
  title: 'Categories | Budget Planner',
});
</script>

<template>
  <Categories/>
</template>
