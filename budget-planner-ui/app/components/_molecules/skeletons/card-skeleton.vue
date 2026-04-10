<script setup lang="ts">
import SkeletonLoader from "~/components/_atoms/animations/skeleton-loader.vue";
</script>

<template>
  <UCard>
    <div class="p-7 space-y-6">
      <div class="flex items-start justify-between">
        <div class="space-y-2 w-1/2">
          <SkeletonLoader class="h-6 w-full" />
        </div>
        <SkeletonLoader class="h-6 w-16 rounded-full" />
      </div>

      <div class="space-y-2">
        <SkeletonLoader class="h-3 w-24" />
        <SkeletonLoader class="h-12 w-48" />
      </div>
    </div>
  </UCard>
</template>
