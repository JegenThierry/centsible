<script lang="ts" setup>
defineProps<{
  label: string;
  description: string;
  icon: string;
  to: string;
}>();
</script>

<template>
  <ULink class="flex items-start gap-3 p-3 rounded-md bg-elevated hover:bg-accented transition-colors"
         :to="to"
         target="_blank">
    <UIcon class="w-5 h-5 text-primary shrink-0 mt-0.5" :name="icon"/>
    <div class="min-w-0">
      <p class="text-sm font-medium text-highlighted">{{ label }}</p>
      <p class="text-xs text-muted">{{ description }}</p>
    </div>
  </ULink>
</template>
