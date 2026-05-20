<script lang="ts" setup>
import {computed} from 'vue';
import {REPORT_RANGE_PRESETS, type ReportRangePreset} from "~/composables/use-report-date-range";

const preset = defineModel<ReportRangePreset>('preset', {required: true});
const customFrom = defineModel<string>('customFrom', {required: true});
const customTo = defineModel<string>('customTo', {required: true});

defineProps<{
  disabled?: boolean;
}>();

const {t} = useI18n();

const presetItems = computed(() => [
  ...REPORT_RANGE_PRESETS.map(p => ({value: p.key, label: t(`reports.ranges.${p.key}`)})),
  {value: 'custom' as ReportRangePreset, label: t('reports.ranges.custom')},
]);
</script>

<template>
  <div class="flex items-center gap-2">
    <USelect v-model="preset"
             :disabled="disabled"
             :items="presetItems"
             class="w-40"
             value-key="value"/>
    <template v-if="preset === 'custom'">
      <UInput v-model="customFrom"
              :disabled="disabled"
              class="w-36"
              type="date"/>
      <span class="text-neutral-400 text-sm">→</span>
      <UInput v-model="customTo"
              :disabled="disabled"
              class="w-36"
              type="date"/>
    </template>
  </div>
</template>
