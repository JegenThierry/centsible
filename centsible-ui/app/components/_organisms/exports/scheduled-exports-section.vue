<script lang="ts" setup>
import {useExportSchedules} from "~/composables/use-export-schedules";
import type {ExportSchedule} from "~/models/export/export-schedule";
import type {ScheduleFormValue} from "~/components/_organisms/exports/export-schedule-modal.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";

const ExportScheduleModal = defineAsyncComponent(() => import("~/components/_organisms/exports/export-schedule-modal.vue"));
const ConfirmationModal = defineAsyncComponent(() => import("~/components/_organisms/modals/confirmation-modal.vue"));

const {t} = useI18n();
const {schedules, loading, submitting, refresh, create, update, toggleActive, remove} = useExportSchedules();

onMounted(refresh);

const isModalOpen = ref(false);
const editing = ref<ExportSchedule | null>(null);
const pendingDeleteId = ref<string | null>(null);
const isDeleteOpen = ref(false);

function openCreate() {
  editing.value = null;
  isModalOpen.value = true;
}

function openEdit(schedule: ExportSchedule) {
  editing.value = schedule;
  isModalOpen.value = true;
}

async function onSave(value: ScheduleFormValue) {
  const ok = editing.value
    ? await update(editing.value.id, value)
    : await create({title: value.title, type: value.type, format: value.format, frequency: value.frequency});
  if (ok) isModalOpen.value = false;
}

function askDelete(id: string) {
  pendingDeleteId.value = id;
  isDeleteOpen.value = true;
}

async function confirmDelete() {
  if (pendingDeleteId.value) await remove(pendingDeleteId.value);
}
</script>

<template>
  <section class="mb-8">
    <div class="flex items-center justify-between gap-3 mb-3">
      <div>
        <h2 class="text-base font-semibold text-highlighted">{{ t('exports.schedules.title') }}</h2>
        <p class="text-xs text-muted">{{ t('exports.schedules.description') }}</p>
      </div>
      <AppButton icon="i-lucide-plus" size="sm" @click="openCreate">
        {{ t('exports.schedules.new') }}
      </AppButton>
    </div>

    <div v-if="schedules.length === 0 && !loading"
         class="rounded-lg border border-dashed border-default px-4 py-6 text-center text-sm text-muted">
      {{ t('exports.schedules.empty') }}
    </div>

    <ul v-else class="divide-y divide-default rounded-lg border border-default overflow-hidden">
      <li v-for="s in schedules" :key="s.id" class="flex flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
        <div class="min-w-0 flex-1">
          <p class="font-medium text-highlighted truncate">{{ s.title }}</p>
          <p class="text-xs text-muted">
            <span class="uppercase">{{ s.format }}</span>
            · {{ t(`exports.schedules.frequency.${s.frequency.toLowerCase()}`) }}
            <template v-if="s.nextRunAt">
              · {{ t('exports.schedules.nextRun') }} <FormattedDate :date="s.nextRunAt" format="date"/>
            </template>
          </p>
        </div>
        <USwitch :model-value="s.active"
                 :disabled="submitting"
                 :aria-label="t('exports.schedules.toggleAria')"
                 @update:model-value="toggleActive(s)"/>
        <div class="flex items-center gap-1">
          <AppButton icon="i-lucide-pencil"
                     color="neutral"
                     variant="ghost"
                     size="xs"
                     :aria-label="t('common.actions.edit')"
                     @click="openEdit(s)"/>
          <AppButton icon="i-lucide-trash"
                     color="error"
                     variant="ghost"
                     size="xs"
                     :aria-label="t('common.actions.delete')"
                     @click="askDelete(s.id)"/>
        </div>
      </li>
    </ul>

    <ExportScheduleModal v-if="isModalOpen"
                         v-model:open="isModalOpen"
                         :schedule="editing"
                         :submitting="submitting"
                         @save="onSave"/>

    <ConfirmationModal v-if="isDeleteOpen"
                       v-model:open="isDeleteOpen"
                       :title="t('common.confirmDelete.title')"
                       :body="t('exports.schedules.deleteConfirm')"
                       :confirm-label="t('common.actions.delete')"
                       :delete-callback="confirmDelete"
                       :manage-toasts="false"/>
  </section>
</template>
