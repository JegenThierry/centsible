<script lang="ts" setup>
interface Props {
  label: string;
  icon: string;
  selected: boolean;
}

defineProps<Props>();
const emit = defineEmits<{
  (e: 'toggle'): void;
}>();
</script>

<template>
  <button :class="['flex items-center gap-2 px-3 py-1.5 rounded-full border text-sm transition-colors',
                   selected
                     ? 'bg-primary-500 border-primary-500 text-white'
                     : 'border-neutral-300 dark:border-neutral-700 hover:border-primary-400']"
          @click="emit('toggle')">
    <UIcon :name="icon" class="w-4 h-4"/>
    {{ label }}
  </button>
</template>
