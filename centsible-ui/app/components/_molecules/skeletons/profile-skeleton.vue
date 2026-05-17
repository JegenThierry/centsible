<script lang="ts" setup>
import SkeletonLoader from "~/components/_atoms/animations/skeleton-loader.vue";
</script>

<template>
  <div class="flex items-center gap-3 px-2 py-1">
    <SkeletonLoader class="w-10 h-10 rounded-full"/>
    <div class="flex flex-col gap-1.5">
      <SkeletonLoader class="h-4 w-24"/>
      <SkeletonLoader class="h-3 w-36"/>
    </div>
  </div>
</template>
