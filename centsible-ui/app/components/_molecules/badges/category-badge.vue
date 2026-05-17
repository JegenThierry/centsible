<script lang="ts" setup>
type Size = 'xs' | 'sm' | 'md' | 'lg';

const props = withDefaults(defineProps<{
  name?: string | null;
  icon?: string | null;
  color?: string | null;
  size?: Size;
}>(), {
  size: 'sm'
});

const {t} = useI18n();

const ICON_SIZE: Record<Size, string> = {
  xs: 'w-3 h-3',
  sm: 'w-3.5 h-3.5',
  md: 'w-4 h-4',
  lg: 'w-4 h-4',
};

const resolvedColor = computed(() => props.color || '#a3a3a3');
const badgeStyle = computed(() => ({
  backgroundColor: `${resolvedColor.value}15`,
  color: resolvedColor.value,
  border: `1px solid ${resolvedColor.value}30`,
}));
</script>

<template>
  <UBadge
    v-if="name"
    :size="size"
    :style="badgeStyle"
    class="flex items-center gap-1.5 w-fit font-medium"
    variant="subtle"
  >
    <UIcon v-if="icon" :class="ICON_SIZE[size]" :name="icon"/>
    <span>{{ name }}</span>
  </UBadge>
  <UBadge v-else :size="size" color="neutral" variant="subtle">
    {{ t('transactions.category.noCategory') }}
  </UBadge>
</template>
