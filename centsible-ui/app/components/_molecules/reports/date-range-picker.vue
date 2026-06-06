<script lang="ts" setup>
import {computed} from 'vue';
import {REPORT_RANGE_PRESETS, type ReportRangePreset} from "~/composables/use-report-date-range";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import AppInput from "~/components/_atoms/ui/app-input.vue";

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
    <AppSelect v-model="preset"
             :disabled="disabled"
             :items="presetItems"
             class="w-40"
             value-key="value"/>
    <template v-if="preset === 'custom'">
      <AppInput v-model="customFrom"
              :disabled="disabled"
              class="w-36"
              type="date"/>
      <span class="text-neutral-400 text-sm">→</span>
      <AppInput v-model="customTo"
              :disabled="disabled"
              class="w-36"
              type="date"/>
    </template>
  </div>
</template>
