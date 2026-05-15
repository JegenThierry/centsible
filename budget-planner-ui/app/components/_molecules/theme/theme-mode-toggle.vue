<script lang="ts" setup>
import type {ThemeMode} from "~/composables/use-theme";

defineProps<{
  modelValue: ThemeMode;
}>();

defineEmits<{
  'update:modelValue': [value: ThemeMode];
}>();

const modes: Array<{value: ThemeMode; label: string; icon: string}> = [
  {value: 'light', label: 'Light', icon: 'i-lucide-sun'},
  {value: 'dark', label: 'Dark', icon: 'i-lucide-moon'},
  {value: 'system', label: 'System', icon: 'i-lucide-monitor'},
];
</script>

<template>
  <div class="grid grid-cols-3 gap-1 p-1 bg-elevated/60 rounded-md ring-1 ring-default">
    <button
      v-for="mode in modes"
      :key="mode.value"
      :class="[
        'flex items-center justify-center gap-1.5 py-1.5 px-2 rounded text-xs font-medium transition-colors',
        modelValue === mode.value
          ? 'bg-default text-highlighted shadow-sm ring-1 ring-default'
          : 'text-muted hover:text-default'
      ]"
      type="button"
      @click="$emit('update:modelValue', mode.value)">
      <UIcon :name="mode.icon" class="w-3.5 h-3.5"/>
      {{ mode.label }}
    </button>
  </div>
</template>
