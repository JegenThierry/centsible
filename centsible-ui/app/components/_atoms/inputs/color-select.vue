<script lang="ts" setup>
const colors = [
  '#ef4444', // Red
  '#f97316', // Orange
  '#eab308', // Yellow
  '#22c55e', // Green
  '#06b6d4', // Cyan
  '#3b82f6', // Blue
  '#a855f7', // Purple
  '#ec4899'  // Pink
];

const model = defineModel<string>({default: '#3b82f6'});

defineProps<{
  label?: string;
  required?: boolean;
}>();
</script>

<template>
  <UFormField :label="label" :required="required">
    <div class="flex flex-wrap gap-3 mt-1 py-1">
      <button
        v-for="color in colors"
        :key="color"
        :class="model === color ? 'ring-2 ring-primary-500 ring-offset-2 dark:ring-offset-gray-900 scale-110' : 'border-gray-200 dark:border-gray-700'"
        :style="{ backgroundColor: color, borderColor: model === color ? 'currentColor' : 'transparent' }"
        class="w-8 h-8 rounded-full border-2 transition-all cursor-pointer hover:scale-110 flex items-center justify-center"
        type="button"
        @click="model = color"
      >
        <UIcon v-if="model === color" class="w-4 h-4 text-white drop-shadow-sm" name="i-lucide-check"/>
      </button>
    </div>
  </UFormField>
</template>
