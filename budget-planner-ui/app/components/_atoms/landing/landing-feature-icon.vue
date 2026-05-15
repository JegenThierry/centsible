<script lang="ts" setup>
export type FeatureIconColor = 'primary' | 'success' | 'warning' | 'info';

const props = withDefaults(defineProps<{
  icon: string;
  color?: FeatureIconColor;
}>(), {
  color: 'primary',
});

const colorClass: Record<FeatureIconColor, string> = {
  primary: 'bg-primary-100 dark:bg-primary-900/30 text-primary',
  success: 'bg-success-100 dark:bg-success-900/30 text-success',
  warning: 'bg-warning-100 dark:bg-warning-900/30 text-warning',
  info: 'bg-info-100 dark:bg-info-900/30 text-info',
};
</script>

<template>
  <div :class="['flex items-center justify-center w-11 h-11 rounded-xl shrink-0', colorClass[color]]">
    <UIcon class="w-6 h-6" :name="icon"/>
  </div>
</template>
