<script lang="ts" setup>
import SkeletonLoader from "~/components/_atoms/animations/skeleton-loader.vue";
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <SkeletonLoader class="h-6 w-48"/>
      </div>
    </template>

    <div class="h-64">
      <SkeletonLoader class="h-full w-full rounded-lg"/>
    </div>
  </UCard>
</template>
