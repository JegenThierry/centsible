<script lang="ts" setup>
const emit = defineEmits<{
  (e: 'refresh'): void;
}>();

function onRefresh() {
  emit('refresh');
}
</script>

<template>
  <UButton color="neutral" icon="i-lucide-refresh-cw" variant="subtle" @click="onRefresh">
    Refresh
  </UButton>
</template>

