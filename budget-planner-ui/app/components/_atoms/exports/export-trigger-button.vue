<script lang="ts" setup>
withDefaults(defineProps<{
  label?: string;
  size?: 'xs' | 'sm' | 'md' | 'lg';
  variant?: 'solid' | 'subtle' | 'ghost' | 'outline';
  icon?: string;
  loading?: boolean;
  disabled?: boolean;
}>(), {
  label: 'Export',
  size: 'sm',
  variant: 'subtle',
  icon: 'i-lucide-file-down',
});

const emit = defineEmits<{ (e: 'trigger'): void }>();
</script>

<template>
  <UButton
    :disabled="disabled"
    :icon="icon"
    :loading="loading"
    :size="size"
    :variant="variant"
    color="primary"
    @click="emit('trigger')"
  >
    {{ label }}
  </UButton>
</template>
