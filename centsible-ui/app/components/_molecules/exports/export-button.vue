<script lang="ts" setup>
import ExportOptionsPopover from "~/components/_organisms/exports/export-options-popover.vue";
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
  variant: 'subtle',
  size: 'sm',
  icon: 'i-lucide-file-down',
});

const {create, submitting} = useExports();
const toasts = useToasts();
const {t} = useI18n();

const resolvedLabel = computed(() => props.label ?? t('exports.trigger.defaultLabel'));

async function onSubmit(payload: CreateExportRequest) {
  const job = await create(payload);
  if (job) {
    toasts.success(t('exports.toasts.startedTitle'), t('exports.toasts.startedBody', {title: payload.title}));
  } else {
    toasts.error(t('exports.toasts.failedTitle'), t('exports.toasts.failedBody'));
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
    <ExportTriggerButton :icon="icon" :label="resolvedLabel" :size="size" :variant="variant"/>
  </ExportOptionsPopover>
</template>
