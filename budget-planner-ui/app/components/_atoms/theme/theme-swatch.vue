<script lang="ts" setup>
withDefaults(defineProps<{
  color: string;
  size?: 'sm' | 'md';
}>(), {
  size: 'md',
});
</script>

<template>
  <span
    :class="[
      'inline-block rounded-full ring-1 ring-default shadow-sm',
      size === 'sm' ? 'w-3 h-3' : 'w-4 h-4'
    ]"
    :style="{ backgroundColor: color }"/>
</template>
