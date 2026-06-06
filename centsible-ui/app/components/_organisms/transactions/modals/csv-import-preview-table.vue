<script lang="ts" setup>
import type {ImportPayloadRow} from "~/models/transactions/csv-import";

defineProps<{
  rows: ImportPayloadRow[];
  invalidCount: number;
}>();

const {t} = useI18n();
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between gap-4 text-sm">
      <p>{{ t('transactions.import.readyToImport', {count: rows.length}) }}</p>
      <p v-if="invalidCount > 0" class="text-warning">
        {{ t('transactions.import.skippedRows', {count: invalidCount}) }}
      </p>
    </div>
    <div class="max-h-64 overflow-auto border border-default rounded-md">
      <table class="w-full text-sm">
        <thead class="bg-muted sticky top-0">
          <tr>
            <th class="text-left p-2">{{ t('transactions.table.date') }}</th>
            <th class="text-left p-2">{{ t('transactions.table.description') }}</th>
            <th class="text-right p-2">{{ t('transactions.table.amount') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in rows.slice(0, 50)" :key="i" class="border-t border-muted">
            <td class="p-2 tabular-nums">{{ row.transactionDate }}</td>
            <td class="p-2 truncate max-w-[20rem]">{{ row.description }}</td>
            <td class="p-2 text-right tabular-nums">{{ row.amount.toFixed(2) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-if="rows.length > 50" class="text-xs text-neutral-500">
      {{ t('transactions.import.previewShowingFirst', {count: rows.length}) }}
    </p>
  </div>
</template>
