<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 sm:mb-8">
    <div>
      <h1 class="text-2xl sm:text-3xl font-bold tracking-tight">{{ title }}</h1>
      <p v-if="description" class="text-sm sm:text-base text-neutral-500 dark:text-neutral-400">
        {{ description }}
      </p>
    </div>
    <div v-if="$slots.actions" class="flex flex-col sm:flex-row gap-3">
      <slot name="actions" />
    </div>
  </div>
</template>
