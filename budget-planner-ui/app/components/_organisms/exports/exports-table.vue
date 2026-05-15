<script lang="ts" setup>
import ExportStatusBadge from "~/components/_atoms/exports/export-status-badge.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import type {ExportJob, ExportType} from "~/models/export/export-job";

defineProps<{
  exports: ExportJob[];
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'download', job: ExportJob): void;
  (e: 'retrigger', jobId: string): void;
  (e: 'delete', jobId: string): void;
}>();

const TYPE_LABELS: Record<ExportType, string> = {
  TRANSACTIONS: 'Transactions',
  LENDINGS_PER_CONTACT: 'Lendings (one contact)',
  LENDINGS_ALL: 'Lendings (all)',
  ACCOUNTS_SUMMARY: 'Accounts summary',
};

function postProcessingLabel(job: ExportJob): string {
  if (job.postProcessing.length === 0) return '—';
  return job.postProcessing.map((p) => `${p.type} (${p.status})`).join(', ');
}
</script>

<template>
  <div class="border rounded-lg overflow-hidden border-neutral-200 dark:border-neutral-800">
    <UTable
      :data="exports"
      :loading="loading"
      :columns="[
        { id: 'title', accessorKey: 'title', header: 'Title' },
        { id: 'type', accessorKey: 'type', header: 'Type' },
        { id: 'status', accessorKey: 'status', header: 'Status' },
        { id: 'postProcessing', header: 'Follow-up' },
        { id: 'createdAt', accessorKey: 'createdAt', header: 'Created' },
        { id: 'completedAt', accessorKey: 'completedAt', header: 'Completed' },
        { id: 'actions', header: '', meta: { class: { td: 'text-right' } } },
      ]"
    >
      <template #title-cell="{ row }">
        <div class="font-medium">{{ row.original.title }}</div>
        <div v-if="row.original.errorMessage" class="text-xs text-error mt-0.5">
          {{ row.original.errorMessage }}
        </div>
      </template>

      <template #type-cell="{ row }">
        <span class="text-sm">{{ TYPE_LABELS[row.original.type] }}</span>
      </template>

      <template #status-cell="{ row }">
        <ExportStatusBadge :status="row.original.status"/>
      </template>

      <template #postProcessing-cell="{ row }">
        <span class="text-xs text-neutral-600 dark:text-neutral-400">
          {{ postProcessingLabel(row.original) }}
        </span>
      </template>

      <template #createdAt-cell="{ row }">
        <FormattedDate :date="row.original.createdAt" class="text-xs"/>
      </template>

      <template #completedAt-cell="{ row }">
        <FormattedDate v-if="row.original.completedAt" :date="row.original.completedAt" class="text-xs"/>
        <span v-else class="text-xs">—</span>
      </template>

      <template #actions-cell="{ row }">
        <div class="flex justify-end gap-1">
          <UButton
            v-if="row.original.status === 'COMPLETED'"
            color="primary"
            icon="i-lucide-download"
            size="xs"
            variant="ghost"
            @click="emit('download', row.original)"
          >
            Download
          </UButton>
          <UButton
            color="neutral"
            icon="i-lucide-refresh-cw"
            size="xs"
            variant="ghost"
            @click="emit('retrigger', row.original.id)"
          >
            Retrigger
          </UButton>
          <UButton
            color="error"
            icon="i-lucide-trash-2"
            size="xs"
            variant="ghost"
            @click="emit('delete', row.original.id)"
          />
        </div>
      </template>
    </UTable>
  </div>
</template>
