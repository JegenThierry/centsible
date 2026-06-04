<script lang="ts" setup>
defineProps<{
  inset?: boolean
}>()
</script>

<template>
  <div class="flex-1 flex flex-col overflow-hidden"
       :class="{ inset : 'sm:m-4 lg:ms-0 sm:rounded-xl sm:shadow-sm sm:ring sm:ring-default' }">
    <slot name="header" />
    <div class="flex-1 overflow-y-auto">
      <slot />
      <slot name="footer" />
    </div>
  </div>
</template>
