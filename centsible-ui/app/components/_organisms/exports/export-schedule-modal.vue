<script lang="ts" setup>
import type {ExportSchedule, ScheduleFrequency} from "~/models/export/export-schedule";
import {SCHEDULE_FREQUENCIES} from "~/models/export/export-schedule";
import type {ExportFormat} from "~/models/export/export-job";
import {EXPORT_FORMATS} from "~/models/export/export-job";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";

export interface ScheduleFormValue {
  title: string;
  format: ExportFormat;
  frequency: ScheduleFrequency;
  active: boolean;
}

const props = defineProps<{
  schedule?: ExportSchedule | null;
  submitting?: boolean;
}>();
const isOpen = defineModel<boolean>('open', {required: true});
const emit = defineEmits<{ (e: 'save', value: ScheduleFormValue): void }>();

const {t} = useI18n();

const title = ref(props.schedule?.title ?? '');
const format = ref<ExportFormat>(props.schedule?.format ?? 'PDF');
const frequency = ref<ScheduleFrequency>(props.schedule?.frequency ?? 'MONTHLY');

const isEdit = computed(() => !!props.schedule);
const canSubmit = computed(() => title.value.trim().length > 0);

const formatOptions = EXPORT_FORMATS.map((value) => ({value, label: value}));
const frequencyOptions = computed(() =>
  SCHEDULE_FREQUENCIES.map((value) => ({value, label: t(`exports.schedules.frequency.${value.toLowerCase()}`)})),
);

function submit() {
  if (!canSubmit.value) return;
  emit('save', {
    title: title.value.trim(),
    format: format.value,
    frequency: frequency.value,
    active: props.schedule?.active ?? true,
  });
}
</script>

<template>
  <UModal :open="isOpen"
          :title="isEdit ? t('exports.schedules.form.editTitle') : t('exports.schedules.form.createTitle')"
          :description="t('exports.schedules.form.description')"
          @update:open="isOpen = $event">
    <template #body>
      <div class="space-y-3">
        <div>
          <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">
            {{ t('exports.schedules.form.nameLabel') }}
          </label>
          <AppInput v-model="title" :placeholder="t('exports.schedules.form.namePlaceholder')" class="w-full mt-1"/>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">
              {{ t('exports.schedules.form.formatLabel') }}
            </label>
            <AppSelect v-model="format" :items="formatOptions" class="w-full mt-1" value-key="value"/>
          </div>
          <div>
            <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">
              {{ t('exports.schedules.form.frequencyLabel') }}
            </label>
            <AppSelect v-model="frequency" :items="frequencyOptions" class="w-full mt-1" value-key="value"/>
          </div>
        </div>

        <p class="text-xs text-muted">{{ t('exports.schedules.form.hint') }}</p>
      </div>
    </template>

    <template #footer>
      <ModalFooterActions :loading="submitting"
                          :disabled="!canSubmit"
                          :submit-label="isEdit ? t('common.actions.save') : t('common.actions.create')"
                          @cancel="isOpen = false"
                          @submit="submit"/>
    </template>
  </UModal>
</template>
