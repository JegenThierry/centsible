<script lang="ts" setup>
const props = withDefaults(defineProps<{
  name?: string | null;
  icon?: string | null;
  color?: string | null;
  size?: 'xs' | 'sm' | 'md' | 'lg';
}>(), {
  size: 'sm'
});

const {t} = useI18n();

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
    :size="size"
    :style="{
      backgroundColor: `${color || defaultColor}15`,
      color: color || defaultColor,
      border: `1px solid ${color || defaultColor}30`,
    }"
    class="flex items-center gap-1.5 w-fit font-medium"
    variant="subtle"
  >
    <UIcon v-if="icon" :class="iconSize" :name="icon"/>
    <span>{{ name }}</span>
  </UBadge>
  <UBadge v-else :size="size" color="neutral" variant="subtle">
    {{ t('transactions.category.noCategory') }}
  </UBadge>
</template>
