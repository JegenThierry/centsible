<script setup lang="ts">
defineProps<{
  title: string
  description?: string
  icon?: string
}>()
</script>

<template>
  <UEmpty
    :title="title"
    :description="description"
    :icon="icon"
    class="py-10 sm:py-20"
  >
    <template v-if="$slots.actions" #actions>
      <slot name="actions" />
    </template>
  </UEmpty>
</template>
