<script lang="ts" setup>
import type {DropdownMenuItem} from '@nuxt/ui';

const props = defineProps<{
  items: DropdownMenuItem[];
  triggerLabel?: string;
  menuLabel?: string;
}>();

const {t} = useI18n();

const resolvedTriggerLabel = computed(() => props.triggerLabel ?? t('transactions.table_meta.openRowActions'));
const resolvedMenuLabel = computed(() => props.menuLabel ?? t('transactions.table_meta.rowActions'));
</script>

<template>
  <UDropdownMenu :items="items" :content="{ align: 'end' }" :aria-label="resolvedMenuLabel">
    <UButton :aria-label="resolvedTriggerLabel" color="neutral" icon="i-lucide-ellipsis-vertical" variant="ghost"/>
  </UDropdownMenu>
</template>
