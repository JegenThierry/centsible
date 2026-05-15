<script lang="ts" setup>
withDefaults(
  defineProps<{
    showWordmark?: boolean;
    size?: 'sm' | 'md' | 'lg';
  }>(),
  {
    showWordmark: true,
    size: 'md',
  }
);

const sizeClasses = {
  sm: {wrap: 'w-7 h-7 rounded-lg', icon: 'w-4 h-4', text: 'text-sm'},
  md: {wrap: 'w-8 h-8 rounded-lg', icon: 'w-[1.125rem] h-[1.125rem]', text: 'text-base'},
  lg: {wrap: 'w-10 h-10 rounded-xl', icon: 'w-6 h-6', text: 'text-lg'},
} as const;
</script>

<template>
  <div class="flex items-center gap-2">
    <div
      :class="[
        'flex items-center justify-center shrink-0 bg-primary text-white shadow-sm',
        sizeClasses[size].wrap
      ]">
      <UIcon :class="sizeClasses[size].icon" name="i-lucide-piggy-bank"/>
    </div>
    <span v-if="showWordmark"
          :class="[
            'font-semibold tracking-tight text-highlighted',
            sizeClasses[size].text
          ]">
      Centsible
    </span>
  </div>
</template>
