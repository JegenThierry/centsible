<script lang="ts" setup>
defineProps<{
  title: string
  description?: string
  icon?: string
}>()
</script>

<template>
  <UEmpty
    :description="description"
    :icon="icon"
    :title="title"
    class="py-10 sm:py-20"
  >
    <template v-if="$slots.actions" #actions>
      <slot name="actions"/>
    </template>
  </UEmpty>
</template>
