<script lang="ts" setup>
import AppButton from "~/components/_atoms/ui/app-button.vue";

defineProps<{
  count: number;
}>();

const emit = defineEmits<{
  (e: 'recategorize'): void;
  (e: 'add-tags'): void;
  (e: 'remove-tags'): void;
  (e: 'delete'): void;
  (e: 'clear'): void;
}>();

const {t} = useI18n();
</script>

<template>
  <div
    class="flex items-center gap-2 mb-3 p-3 rounded-lg bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800">
    <span class="text-sm font-medium">{{ t('transactions.bulk.selectedCount', {count}) }}</span>
    <div class="ml-auto flex gap-2">
      <AppButton color="primary" icon="i-lucide-tag" size="sm" variant="outline" @click="emit('recategorize')">
        {{ t('transactions.bulk.recategorize') }}
      </AppButton>
      <AppButton color="primary" icon="i-lucide-plus" size="sm" variant="outline" @click="emit('add-tags')">
        {{ t('transactions.bulk.addTags') }}
      </AppButton>
      <AppButton color="primary" icon="i-lucide-minus" size="sm" variant="outline" @click="emit('remove-tags')">
        {{ t('transactions.bulk.removeTags') }}
      </AppButton>
      <AppButton color="error" icon="i-lucide-trash" size="sm" variant="outline" @click="emit('delete')">
        {{ t('transactions.bulk.delete') }}
      </AppButton>
      <AppButton color="neutral" size="sm" variant="ghost" @click="emit('clear')">
        {{ t('transactions.bulk.clear') }}
      </AppButton>
    </div>
  </div>
</template>
