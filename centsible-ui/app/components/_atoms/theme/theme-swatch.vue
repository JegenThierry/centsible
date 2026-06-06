<script lang="ts" setup>
const props = withDefaults(defineProps<{
  color: string;
  size?: 'sm' | 'md';
}>(), {
  size: 'md',
});

const sizeToClass: Record<string, string> = {
  'sm': 'w-3 h-3',
  'md': 'w-4 h-4'
}

const sizeClass = computed(() => sizeToClass[props.size])
</script>

<template>
  <span :class="`inline-block rounded-full ring-1 ring-default shadow-sm ${sizeClass}`"
        :style="{ backgroundColor: color }"/>
</template>
