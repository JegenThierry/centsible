<script lang="ts" setup>
import {type ImportColumnField} from "~/models/transactions/csv-import";
import AppSelect from "~/components/_atoms/ui/app-select.vue";

defineProps<{
  header: string;
  preview: string;
}>();

const model = defineModel<ImportColumnField>({required: true});

const {t} = useI18n();

const options = computed(() => (['date', 'amount', 'description', 'category', 'ignore'] as ImportColumnField[]).map(value => ({
  label: t(`transactions.import.columnFields.${value}`),
  value,
})));
</script>

<template>
  <div class="grid grid-cols-1 sm:grid-cols-3 items-center gap-3 py-2 border-b border-neutral-200 dark:border-neutral-800 last:border-b-0">
    <div class="text-sm font-medium truncate">{{ header }}</div>
    <div class="text-xs text-neutral-500 truncate sm:col-span-1">
      <span class="text-neutral-400">{{ t('transactions.import.previewExample') }}</span> {{ preview || '—' }}
    </div>
    <AppSelect v-model="model"
             :items="options"
             class="w-full"
             value-key="value"/>
  </div>
</template>
