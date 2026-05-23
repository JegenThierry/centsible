<script lang="ts" setup>
defineProps<{
  editAriaLabel?: string;
  deleteAriaLabel?: string;
  hideOnHover?: boolean;
}>();

const emit = defineEmits<{
  (e: 'edit'): void;
  (e: 'delete'): void;
}>();
</script>

<template>
  <div :class="['flex items-center gap-1', hideOnHover && 'opacity-0 group-hover:opacity-100 transition-opacity']">
    <UButton :aria-label="editAriaLabel"
             color="neutral"
             icon="i-lucide-pencil"
             size="sm"
             variant="ghost"
             @click.stop="emit('edit')"/>
    <UButton :aria-label="deleteAriaLabel"
             color="error"
             icon="i-lucide-trash"
             size="sm"
             variant="ghost"
             @click.stop="emit('delete')"/>
  </div>
</template>
