<script lang="ts" setup>
import Integrations from "~/components/_organisms/integrations/integrations.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

useHead({
  title: 'Integrations | Budget Planner',
});
</script>

<template>
  <Integrations/>
</template>
