<script lang="ts" setup>
const emit = defineEmits<{
  (e: 'create'): void;
}>();

function onCreate() {
  emit('create');
}
</script>

<template>
  <UButton aria-label="Create"
           class="fixed bottom-12 right-12 rounded-full shadow-lg z-50"
           color="primary"
           icon="i-lucide-plus"
           size="xl"
           @click="onCreate"/>
</template>
