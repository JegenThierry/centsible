<script lang="ts" setup>
import Landing from "~/components/_organisms/landing/landing.vue";

definePageMeta({
  middleware: ['landing-guard'],
});

useHead({
  title: 'Centsible',
});
</script>

<template>
  <Landing/>
</template>
