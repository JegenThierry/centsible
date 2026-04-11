<script setup lang="ts">
defineProps<{
  loading?: boolean;
}>();
</script>

<template>
  <UButton color="error" :loading="loading" v-bind="$attrs">
    <slot>Delete</slot>
  </UButton>
</template>
