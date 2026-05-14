<script lang="ts" setup>
import ExportOptionsPopover from "~/components/_molecules/exports/export-options-popover.vue";
import ExportTriggerButton from "~/components/_atoms/exports/export-trigger-button.vue";
import {useExports} from "~/composables/use-exports";
import {useToasts} from "~/services/toasts/toast-service";
import type {CreateExportRequest, ExportRequestParams, ExportType} from "~/models/export/export-job";

const props = withDefaults(defineProps<{
  type: ExportType;
  defaultTitle: string;
  paramsBuilder: () => ExportRequestParams;
  label?: string;
  variant?: 'solid' | 'subtle' | 'ghost' | 'outline';
  size?: 'xs' | 'sm' | 'md' | 'lg';
  icon?: string;
}>(), {
  label: 'Export',
  variant: 'subtle',
  size: 'sm',
  icon: 'i-lucide-file-down',
});

const {create, submitting} = useExports();
const toasts = useToasts();

async function onSubmit(payload: CreateExportRequest) {
  const job = await create(payload);
  if (job) {
    toasts.success('Export started', `${payload.title} is being prepared. Check My Documents when it\'s ready.`);
  } else {
    toasts.error('Export failed', 'Could not start the export. Please try again.');
  }
}
</script>

<template>
  <ExportOptionsPopover
    :default-title="defaultTitle"
    :params-builder="paramsBuilder"
    :pending="submitting"
    :type="type"
    @submit="onSubmit"
  >
    <ExportTriggerButton :icon="icon" :label="label" :size="size" :variant="variant"/>
  </ExportOptionsPopover>
</template>
