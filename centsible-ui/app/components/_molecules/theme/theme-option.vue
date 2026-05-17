<script lang="ts" setup>
import ThemeSwatch from "~/components/_atoms/theme/theme-swatch.vue";

defineProps<{
  label: string;
  color: string;
  selected: boolean;
}>();

defineEmits<{
  select: [];
}>();
</script>

<template>
  <button
    :class="[
      'w-full flex items-center gap-2.5 px-2.5 py-2 rounded-md text-left transition-colors',
      selected
        ? 'bg-primary/10 text-primary ring-1 ring-primary/30'
        : 'hover:bg-primary/5 text-default'
    ]"
    type="button"
    @click="$emit('select')">
    <ThemeSwatch :color="color"/>
    <span class="flex-1 text-sm font-medium truncate">{{ label }}</span>
    <UIcon v-if="selected"
           class="w-4 h-4 text-primary shrink-0"
           name="i-lucide-check"/>
  </button>
</template>
