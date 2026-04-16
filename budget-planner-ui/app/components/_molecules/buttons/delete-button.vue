<script lang="ts" setup>
defineProps<{
  loading?: boolean;
}>();
</script>

<template>
  <UButton :loading="loading" color="error" v-bind="$attrs">
    <slot>Delete</slot>
  </UButton>
</template>
