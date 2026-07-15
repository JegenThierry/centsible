<script lang="ts" setup>
import ExportStatusBadge from "~/components/_atoms/exports/export-status-badge.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import BaseTable from "~/components/_molecules/tables/base-table.vue";
import TableRowActionsMenu from "~/components/_molecules/tables/table-row-actions-menu.vue";
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

const {t} = useI18n();

const typeLabel = (type: ExportType): string => t(`exports.types.${type}`);

/** The export worker has no MessageSource, so a LocalizedException reaches `errorMessage` as its raw bundle key. */
const ERROR_KEY_MAP: Record<string, string> = {
  'error.export.tooManyTransactions': 'exports.errors.tooManyTransactions',
};

const errorText = (message: string): string => {
  const key = ERROR_KEY_MAP[message];
  return key ? t(key) : message;
};

const columns = computed(() => [
  {id: 'title', accessorKey: 'title', header: t('exports.table.title')},
  {id: 'type', accessorKey: 'type', header: t('exports.table.type')},
  {id: 'status', accessorKey: 'status', header: t('exports.table.status')},
  {id: 'postProcessing', header: t('exports.table.followUp')},
  {id: 'createdAt', accessorKey: 'createdAt', header: t('exports.table.created')},
  {id: 'completedAt', accessorKey: 'completedAt', header: t('exports.table.completed')},
  {id: 'actions', header: '', meta: {class: {td: 'text-right'}}},
]);

function postProcessingLabel(job: ExportJob): string {
  if (job.postProcessing.length === 0) return '—';
  return job.postProcessing.map((p) => `${p.type} (${p.status})`).join(', ');
}
</script>

<template>
  <BaseTable
    :data="exports"
    :loading="loading"
    :empty-title="t('exports.table.empty')"
    :columns="columns"
  >
    <template #title-cell="{ row }">
      <div class="font-medium">{{ row.original.title }}</div>
      <div v-if="row.original.errorMessage" class="text-xs text-error mt-0.5">
        {{ errorText(row.original.errorMessage) }}
      </div>
    </template>

    <template #type-cell="{ row }">
      <span class="text-sm">{{ typeLabel(row.original.type) }}</span>
    </template>

    <template #status-cell="{ row }">
      <ExportStatusBadge :status="row.original.status"/>
    </template>

    <template #postProcessing-cell="{ row }">
      <span class="text-xs text-muted">
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
      <TableRowActionsMenu
        :menu-label="t('exports.table.actionsAria')"
        :items="[
          {
            label: t('exports.table.download'),
            icon: 'i-lucide-download',
            disabled: row.original.status !== 'COMPLETED',
            onSelect: () => emit('download', row.original),
          },
          {
            label: t('exports.table.retrigger'),
            icon: 'i-lucide-refresh-cw',
            onSelect: () => emit('retrigger', row.original.id),
          },
          {
            label: t('exports.table.delete'),
            icon: 'i-lucide-trash-2',
            color: 'error',
            onSelect: () => emit('delete', row.original.id),
          },
        ]"
      />
    </template>
  </BaseTable>
</template>
