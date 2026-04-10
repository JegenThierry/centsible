<script setup lang="ts">
import SkeletonLoader from "~/components/_atoms/animations/skeleton-loader.vue";
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <SkeletonLoader class="h-6 w-32" />
      </div>
    </template>

    <div class="space-y-4">
      <div v-for="i in 5" :key="i" class="flex justify-between items-center py-3 border-b border-neutral-200 dark:border-neutral-800 last:border-0">
        <SkeletonLoader class="h-4 w-24" />
        <SkeletonLoader class="h-4 w-16" />
      </div>
    </div>
  </UCard>
</template>
