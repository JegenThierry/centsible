<script lang="ts" setup>
import type {ExportStatus} from "~/models/export/export-job";

const props = defineProps<{
  status: ExportStatus;
}>();

const {t} = useI18n();

const presentation = computed(() => {
  switch (props.status) {
    case 'PENDING':
      return {label: t('exports.status.PENDING'), color: 'neutral' as const, icon: 'i-lucide-clock'};
    case 'IN_PROGRESS':
      return {label: t('exports.status.IN_PROGRESS'), color: 'info' as const, icon: 'i-lucide-loader-2'};
    case 'COMPLETED':
      return {label: t('exports.status.COMPLETED'), color: 'success' as const, icon: 'i-lucide-check'};
    case 'FAILED':
      return {label: t('exports.status.FAILED'), color: 'error' as const, icon: 'i-lucide-x'};
  }
});
</script>

<template>
  <UBadge :color="presentation.color" :icon="presentation.icon" size="sm" variant="subtle">
    {{ presentation.label }}
  </UBadge>
</template>
