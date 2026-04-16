<script lang="ts" setup>
import SkeletonLoader from "~/components/_atoms/animations/skeleton-loader.vue";
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <SkeletonLoader class="h-6 w-32"/>
      </div>
    </template>

    <div class="overflow-y-auto max-h-64">
      <div class="divide-y divide-neutral-200 dark:divide-neutral-800">
        <div v-for="i in 5" :key="i" class="py-2 sm:py-3 flex justify-between items-center">
          <SkeletonLoader class="h-4 w-24"/>
          <SkeletonLoader class="h-4 w-16"/>
        </div>
      </div>
    </div>
  </UCard>
</template>
