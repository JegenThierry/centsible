<script setup lang="ts">
import Categories from "~/components/categories/categories.vue";

definePageMeta({
  middleware: ['auth-guard']
})

useHead({
  title: 'Categories | Budget Planner',
});
</script>

<template>
  <Categories />
</template>
