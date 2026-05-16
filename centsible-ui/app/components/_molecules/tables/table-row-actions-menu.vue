<script lang="ts" setup>
import type {DropdownMenuItem} from '@nuxt/ui';

const props = defineProps<{
  items: DropdownMenuItem[];
  label?: string;
}>();

const {t} = useI18n();

const triggerLabel = computed(() => props.label ?? t('transactions.table_meta.openRowActions'));
const menuLabel = computed(() => props.label ?? t('transactions.table_meta.rowActions'));
</script>

<template>
  <UDropdownMenu :items="items" :content="{ align: 'end' }" :aria-label="menuLabel">
    <UButton :aria-label="triggerLabel" color="neutral" icon="i-lucide-ellipsis-vertical" variant="ghost"/>
  </UDropdownMenu>
</template>
