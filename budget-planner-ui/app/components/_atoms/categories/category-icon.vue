<script lang="ts" setup>
const props = withDefaults(defineProps<{
  icon?: string | null;
  color?: string | null;
  size?: 'xs' | 'sm' | 'md' | 'lg';
}>(), {
  size: 'md'
});

const sizeClasses = {
  xs: 'w-3.5 h-3.5',
  sm: 'w-4 h-4',
  md: 'w-5 h-5',
  lg: 'w-6 h-6'
};

const defaultColor = '#a3a3a3';
const defaultIcon = 'i-lucide-circle-help';
</script>

<template>
  <div :style="{ backgroundColor: `${props.color || defaultColor}15`, color: props.color || defaultColor }"
       class="p-2 rounded-lg shrink-0 flex items-center justify-center">
    <UIcon :class="sizeClasses[props.size]" :name="props.icon || defaultIcon"/>
  </div>
</template>
