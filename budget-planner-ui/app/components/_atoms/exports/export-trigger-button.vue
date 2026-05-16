<script lang="ts" setup>
const {t} = useI18n();

const props = withDefaults(defineProps<{
  label?: string;
  size?: 'xs' | 'sm' | 'md' | 'lg';
  variant?: 'solid' | 'subtle' | 'ghost' | 'outline';
  icon?: string;
  loading?: boolean;
  disabled?: boolean;
}>(), {
  size: 'sm',
  variant: 'subtle',
  icon: 'i-lucide-file-down',
});

const resolvedLabel = computed(() => props.label ?? t('exports.trigger.defaultLabel'));

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
    {{ resolvedLabel }}
  </UButton>
</template>
