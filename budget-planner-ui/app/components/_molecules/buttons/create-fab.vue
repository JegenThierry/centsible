<script setup lang="ts">
const emit = defineEmits<{
  (e: 'create'): void;
}>()

function onCreate() {
  emit('create');
}
</script>

<template>
  <UButton icon="i-lucide-plus"
           size="xl"
           color="primary"
           class="fixed bottom-8 right-8 rounded-full shadow-lg z-50"/>
</template>
