<script lang="ts" setup>
import type {DropdownMenuItem} from '@nuxt/ui';

const props = defineProps<{
  items: DropdownMenuItem[];
  label?: string;
}>();

const triggerLabel = computed(() => props.label ?? 'Open row actions');
const menuLabel = computed(() => props.label ?? 'Row actions');
</script>

<template>
  <UDropdownMenu :items="items" :content="{ align: 'end' }" :aria-label="menuLabel">
    <UButton :aria-label="triggerLabel" color="neutral" icon="i-lucide-ellipsis-vertical" variant="ghost"/>
  </UDropdownMenu>
</template>
