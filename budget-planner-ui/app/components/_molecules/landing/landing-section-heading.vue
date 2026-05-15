<script lang="ts" setup>
defineProps<{
  eyebrow?: string;
  title: string;
  subtitle?: string;
  align?: 'left' | 'center';
}>();
</script>

<template>
  <div :class="[
        'flex flex-col gap-3 max-w-2xl',
        align === 'center' ? 'mx-auto text-center items-center' : ''
      ]">
    <UBadge v-if="eyebrow"
            color="primary"
            size="sm"
            variant="subtle"
            :label="eyebrow"/>
    <h2 class="text-2xl sm:text-3xl md:text-4xl font-bold tracking-tight text-highlighted">
      {{ title }}
    </h2>
    <p v-if="subtitle" class="text-base sm:text-lg text-muted leading-relaxed">
      {{ subtitle }}
    </p>
  </div>
</template>
