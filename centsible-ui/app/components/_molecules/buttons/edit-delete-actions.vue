<script lang="ts" setup>
import AppButton from "~/components/_atoms/ui/app-button.vue";

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
    <AppButton :aria-label="editAriaLabel"
               color="neutral"
               icon="i-lucide-pencil"
               size="sm"
               variant="ghost"
               @click.stop="emit('edit')"/>
    <AppButton :aria-label="deleteAriaLabel"
               color="error"
               icon="i-lucide-trash"
               size="sm"
               variant="ghost"
               @click.stop="emit('delete')"/>
  </div>
</template>
