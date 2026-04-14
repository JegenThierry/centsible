<script setup lang="ts">
const props = withDefaults(defineProps<{
  name?: string | null;
  icon?: string | null;
  color?: string | null;
  size?: 'xs' | 'sm' | 'md' | 'lg';
}>(), {
  size: 'sm'
});

const defaultColor = '#a3a3a3';
const iconSize = computed(() => {
  if (props.size === 'xs') return 'w-3 h-3';
  if (props.size === 'sm') return 'w-3.5 h-3.5';
  return 'w-4 h-4';
});
</script>

<template>
  <UBadge
    v-if="name"
    :style="{
      backgroundColor: `${color || defaultColor}15`,
      color: color || defaultColor,
      border: `1px solid ${color || defaultColor}30`,
    }"
    :size="size"
    class="flex items-center gap-1.5 w-fit font-medium"
    variant="subtle"
  >
    <UIcon v-if="icon" :name="icon" :class="iconSize"/>
    <span>{{ name }}</span>
  </UBadge>
  <UBadge v-else variant="subtle" color="neutral" :size="size">
    No category
  </UBadge>
</template>
