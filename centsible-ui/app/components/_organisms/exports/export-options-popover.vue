<script lang="ts" setup>
import {computed, ref} from 'vue';
import {useAuthStore} from "~/stores/authStore";
import type {CreateExportRequest, ExportFormat, ExportRequestParams, ExportType} from "~/models/export/export-job";
import {EXPORT_FORMATS} from "~/models/export/export-job";
import {type DateRangePreset, DATE_RANGE_PRESETS, resolvePreset} from "~/utils/date-range";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import AppCheckbox from "~/components/_atoms/ui/app-checkbox.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const props = defineProps<{
  type: ExportType;
  defaultTitle: string;
  paramsBuilder: () => ExportRequestParams;
  pending?: boolean;
}>();

const emit = defineEmits<{
  (e: 'submit', payload: CreateExportRequest): void;
}>();

const authStore = useAuthStore();
const {t} = useI18n();
const open = ref(false);
const sendEmail = ref(false);
const recipient = ref('');
const title = ref('');
const preset = ref<DateRangePreset>('CURRENT_MONTH');
const customFrom = ref('');
const customTo = ref('');
const format = ref<ExportFormat>('PDF');

const userEmail = computed(() => authStore.user?.email ?? '');
const supportsDateRange = computed(() => props.type === 'TRANSACTIONS');
const presetOptions = computed(() =>
  DATE_RANGE_PRESETS.map((value) => ({value, label: t(`exports.options.presets.${value}`)}))
);
const formatOptions = computed(() =>
  EXPORT_FORMATS.map((value) => ({value, label: value}))
);

function reset() {
  sendEmail.value = false;
  recipient.value = '';
  title.value = props.defaultTitle;
  preset.value = 'CURRENT_MONTH';
  customFrom.value = '';
  customTo.value = '';
  format.value = 'PDF';
}

function onOpen(value: boolean) {
  open.value = value;
  if (value) reset();
}

function dateRangeFromSelection() {
  if (preset.value === 'CUSTOM') {
    return {fromDate: customFrom.value || null, toDate: customTo.value || null};
  }
  return resolvePreset(preset.value);
}

function buildParams(): ExportRequestParams {
  const base = props.paramsBuilder();
  if (supportsDateRange.value && base.kind === 'TRANSACTIONS') {
    const {fromDate, toDate} = dateRangeFromSelection();
    return {...base, fromDate, toDate};
  }
  return base;
}

function submit() {
  const payload: CreateExportRequest = {
    type: props.type,
    title: title.value || props.defaultTitle,
    params: buildParams(),
    format: format.value,
    postProcessing: sendEmail.value
      ? [{type: 'SEND_EMAIL', config: recipient.value ? {recipient: recipient.value} : {}}]
      : [],
  };
  emit('submit', payload);
  open.value = false;
}
</script>

<template>
  <UPopover :open="open" @update:open="onOpen">
    <slot/>

    <template #content>
      <div class="p-4 space-y-3 w-80">
        <div>
          <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('exports.options.titleLabel') }}</label>
          <AppInput v-model="title" :placeholder="defaultTitle" class="w-full mt-1"/>
        </div>

        <div>
          <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('exports.options.formatLabel') }}</label>
          <AppSelect v-model="format" :items="formatOptions" class="w-full mt-1" value-key="value"/>
        </div>

        <div v-if="supportsDateRange">
          <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('exports.options.rangeLabel') }}</label>
          <AppSelect v-model="preset" :items="presetOptions" class="w-full mt-1"/>
        </div>

        <div v-if="supportsDateRange && preset === 'CUSTOM'" class="grid grid-cols-2 gap-2">
          <div>
            <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('exports.options.fromLabel') }}</label>
            <AppInput v-model="customFrom" class="w-full mt-1" type="date"/>
          </div>
          <div>
            <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('exports.options.toLabel') }}</label>
            <AppInput v-model="customTo" class="w-full mt-1" type="date"/>
          </div>
        </div>

        <div class="border-t border-neutral-200 dark:border-neutral-800 pt-3">
          <AppCheckbox v-model="sendEmail" :label="t('exports.options.emailMe')"/>
        </div>

        <div v-if="sendEmail">
          <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('exports.options.recipientLabel') }}</label>
          <AppInput v-model="recipient" :placeholder="userEmail" class="w-full mt-1" type="email"/>
          <p class="text-xs text-neutral-500 mt-1">{{ t('exports.options.recipientHelp') }}</p>
        </div>

        <div class="flex justify-end gap-2 pt-2">
          <AppButton color="neutral" variant="ghost" @click="open = false">{{ t('exports.options.cancel') }}</AppButton>
          <AppButton :loading="pending" color="primary" @click="submit">{{ t('exports.options.start') }}</AppButton>
        </div>
      </div>
    </template>
  </UPopover>
</template>
